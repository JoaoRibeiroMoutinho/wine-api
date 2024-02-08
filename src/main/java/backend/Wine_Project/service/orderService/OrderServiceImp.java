package backend.Wine_Project.service.orderService;

import backend.Wine_Project.converter.OrderConverter;
import backend.Wine_Project.dto.orderDto.OrderCreateDto;
import backend.Wine_Project.dto.orderDto.OrderGetDto;
import backend.Wine_Project.dto.orderDto.OrderUpdateDto;
import backend.Wine_Project.exceptions.ShoppingCartAlreadyBeenOrderedException;
import backend.Wine_Project.exceptions.notFound.OrderIdNotFoundException;
import backend.Wine_Project.exceptions.notFound.PdfNotFoundException;
import backend.Wine_Project.model.Item;
import backend.Wine_Project.model.Order;
import backend.Wine_Project.model.ShoppingCart;
import backend.Wine_Project.repository.OrderRepository;
import backend.Wine_Project.service.EmailService;
import backend.Wine_Project.service.LMStudioService;
import backend.Wine_Project.service.shopppingCartService.ShoppingCartServiceImp;
import backend.Wine_Project.util.Messages;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;


@Service
public class OrderServiceImp implements OrderService {

    private final OrderRepository orderRepository;
    private final ShoppingCartServiceImp shoppingCartService;
    private final EmailService emailService;

    private final LMStudioService lmStudioService;

    @Autowired
    public OrderServiceImp(OrderRepository orderRepository, ShoppingCartServiceImp shoppingCartService, EmailService emailService, LMStudioService lmStudioService) {
        this.orderRepository = orderRepository;
        this.shoppingCartService = shoppingCartService;
        this.emailService = emailService;
        this.lmStudioService = lmStudioService;
    }

    @Override
    public List<OrderGetDto> getAll() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(OrderConverter::fromModelToOrderGetDto).toList();
    }

    @Override
    public Long create(OrderCreateDto order) {

        ShoppingCart shoppingCart = shoppingCartService.getById(order.shoppingCartId());

        if (shoppingCart.isOrdered()) {
            throw new ShoppingCartAlreadyBeenOrderedException(Messages.SHOPPING_CART_ALREADY_ORDERED.getMessage());
        }

        Order newOrder = new Order(shoppingCart);
        orderRepository.save(newOrder);

        shoppingCartService.closeShoppingCart(shoppingCart);

        orderRepository.save(newOrder);

        return newOrder.getId();

    }
    @Override
    public void generatePdfInvoice(Order order) {

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(order.getInvoicePath()));
        } catch (DocumentException | FileNotFoundException e) {
            throw new PdfNotFoundException(e.getMessage());
        }

        document.open();

        try {
            document.add(new Paragraph(printInvoice(order.getShoppingCart())));
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
        document.close();
    }
    @Override
    public String printInvoice(ShoppingCart shoppingCart) {

        Date todayDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        // Header
        String invoiceHead = centerAlignText("****************************************************\n");
        String invoiceNum = centerAlignText("\t\t\t\tInvoice " + shoppingCart.getId() + "\n");
        String invoiceDate = centerAlignText("\t\t\t\tDate: " + dateFormat.format(todayDate) + "\n");
        String invoiceHead2 = centerAlignText("****************************************************\n\n");

        // Client data
        String clientData = "Client: " + shoppingCart.getClient().getName() + "\n" +
                "NIF: " + shoppingCart.getClient().getNif() + "\n\n";

        //Product Header
        String newHeader = String.format("%-50s| %-12s| %-15s| %s\n", "Product", "Quantity", "Unit Price", "Total Price");
        String headerLimit = "-".repeat(newHeader.length()) + "\n";


        // Build invoice text
        // Calculate maximum width for each column
        int maxProductNameWidth = 0;
        int maxQuantityWidth = 0;
        int maxUnitPriceWidth = 0;

        for (Item item : shoppingCart.getItems()) {
            maxProductNameWidth = Math.max(maxProductNameWidth, item.getWine().getName().length());
            maxQuantityWidth = Math.max(maxQuantityWidth, String.valueOf(item.getQuantity()).length());
            maxUnitPriceWidth = Math.max(maxUnitPriceWidth, String.valueOf(item.getWine().getPrice()).length());
        }

// Build invoice text
        StringBuilder invoiceText = new StringBuilder();
        for (Item item : shoppingCart.getItems()) {
            String productName = String.format("%-" + (maxProductNameWidth + 2), item.getWine().getName());
            String quantity = String.format("%-" + (maxQuantityWidth + 2), item.getQuantity());
            String unitPrice = String.format("%-" + (maxUnitPriceWidth + 2), item.getWine().getPrice());
            String totalPrice = "" + item.getTotalPrice();
            String line = productName + quantity + unitPrice + totalPrice + "\n";
            invoiceText.append(line);
        }

        // Total amount
        String totalAmount = "\nTotal Amount: " + shoppingCart.getTotalAmount()+"\n".repeat(5);

        String lastQuote = lmStudioService.callLocalLMStudioForQuote("Give me a small inspirational farewell quote.");

        try {
            JSONObject jsonObject = new JSONObject(lastQuote);
            JSONArray choicesArray = jsonObject.getJSONArray("choices");
            JSONObject firstChoice = choicesArray.getJSONObject(0);
            lastQuote = firstChoice.getString("text");

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return invoiceHead + invoiceNum + invoiceDate + invoiceHead2 + clientData + newHeader + headerLimit + invoiceText.toString() + totalAmount + lastQuote;

    }

    private String centerAlignText(String text) {
        int lineLength = text.split("\n")[0].length(); // Get the length of the first line
        int pageWidth = 120; // Assuming a standard page width
        int leftPadding = (pageWidth - lineLength) / 2;
        return " ".repeat(leftPadding) + text; // Add left padding
    }

    @Override
    public void updateOrder(Long id, OrderUpdateDto order){

        Optional<Order> orderOptional = orderRepository.findById(id);
        if (orderOptional.isEmpty()) {
            throw new OrderIdNotFoundException(Messages.ORDER_ID_NOT_FOUND.getMessage());
        }

        Order orderToUpdate = orderOptional.get();

        orderToUpdate.setPaid(order.isPaid());
        orderRepository.save(orderToUpdate);
        if(orderToUpdate.isPaid()){

            String path = "src/main/java/backend/Wine_Project/invoices/invoice_"+orderToUpdate.getId()+".pdf";
            orderToUpdate.setInvoicePath(path);
            try {
                generatePdfInvoice(orderToUpdate);
            } catch (PdfNotFoundException e) {
                throw new PdfNotFoundException(e.getMessage());
            }

            emailService.sendEmailWithAttachment(orderToUpdate.getClient().getEmail(),
                    path, "invoice_"+orderToUpdate.getId()+".pdf",orderToUpdate.getClient().getName());
        }

        orderRepository.save(orderToUpdate);
    }

}
