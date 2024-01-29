package backend.Wine_Project.service.clientService;

import backend.Wine_Project.dto.clientDto.ClientCreateDto;
import backend.Wine_Project.dto.clientDto.ClientReadDto;
import backend.Wine_Project.converter.ClientConverter;
import backend.Wine_Project.exceptions.ClientIdNotFoundException;
import backend.Wine_Project.exceptions.EmailAlreadyExistsException;
import backend.Wine_Project.model.Client;
import backend.Wine_Project.repository.ClientRepository;
import backend.Wine_Project.util.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public class ClientServiceImp implements ClientService {

    private final ClientRepository clientRepository;
    @Autowired
    public ClientServiceImp(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }




    @Override
    public List<ClientReadDto> getAll() {
        List<Client> clients = clientRepository.findAll();
        return ClientConverter.fromModelListToClientReadDtoList(clients);
    }

    @Override
    public Long create(ClientCreateDto client) {
        Optional<Client> clientOptional = this.clientRepository.findClientByEmail(client.email());
        if (clientOptional.isPresent())
            throw new EmailAlreadyExistsException(Messages.CLIENT_EMAIL_ALREADY_EXISTS.getMessage());
        Client newClient = ClientConverter.fromClientCreateDtoToModel(client);
        clientRepository.save(newClient);
        return newClient.getId();
    }

    @Override
    public Client getById(Long id) {
        Optional<Client> optionalClient = clientRepository.findById(id);
        if (optionalClient.isEmpty()) {
            throw new ClientIdNotFoundException(Messages.CLIENT_ID_NOT_FOUND.getMessage() + id);
        }
        return optionalClient.get();
    }

    @Override
    public List<ClientCreateDto> createCostumers(List<ClientCreateDto> clients) {
        for (ClientCreateDto client: clients) {
            Optional<Client> clientOptional = this.clientRepository.findClientByEmail(client.email());
            if (clientOptional.isPresent())
                throw new EmailAlreadyExistsException(Messages.CLIENT_EMAIL_ALREADY_EXISTS.getMessage());
            Client newClient = ClientConverter.fromClientCreateDtoToModel(client);
            clientRepository.save(newClient);
        }
        return clients;
    }




}
