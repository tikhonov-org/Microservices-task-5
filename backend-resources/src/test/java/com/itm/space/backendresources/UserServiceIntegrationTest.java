package com.itm.space.backendresources;


import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.service.UserService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserServiceIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private Keycloak keycloakClient;

    @Autowired
    private UserService userService;

    @Test
    void createUser_ValidUserRepresentation() {

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        Response mockResponse = mock(Response.class);

        when(keycloakClient.realm(any())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any())).thenReturn(mockResponse);

        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(mockResponse.getHeaderString("Location")).thenReturn("/users/123");

        UserRequest request = new UserRequest(
                "test",
                "test@test.com",
                "test",
                "test",
                "test"
        );

        userService.createUser(request);

        verify(usersResource).create(argThat(user ->
                user.getUsername().equals("test") &&
                user.getEmail().equals("test@test.com") &&
                user.getFirstName().equals("test") &&
                user.getLastName().equals("test") &&
                user.isEnabled() &&
                user.getCredentials() != null
        ));
    }

    @Test
    void createUser_ValidException(){

        when(keycloakClient.realm(any())).thenThrow(new WebApplicationException());

        assertThrows(BackendResourcesException.class, () -> {
            UserRequest request = new UserRequest(
                    "test",
                    "test@example.com",
                    "test",
                    "test",
                    "test"
            );
            userService.createUser(request);
        });
    }

    @Test
    void getUserById_Valid() {
        UUID userId = UUID.randomUUID();

        // мок кейклока
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        when(keycloakClient.realm(any())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId.toString())).thenReturn(userResource);

        // мок юзера
        UserRepresentation userRep = new UserRepresentation();
        userRep.setId(userId.toString());
        userRep.setUsername("test");
        userRep.setEmail("test@test.com");
        userRep.setFirstName("test");
        userRep.setLastName("test");
        when(userResource.toRepresentation()).thenReturn(userRep);

        // мок ролей
        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        MappingsRepresentation mappingsRep = mock(MappingsRepresentation.class);
        List<RoleRepresentation> roles = List.of(new RoleRepresentation("test_role", null, false));
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.getAll()).thenReturn(mappingsRep);
        when(mappingsRep.getRealmMappings()).thenReturn(roles);

        // мок групп
        GroupRepresentation groupRep = new GroupRepresentation();
        groupRep.setId(UUID.randomUUID().toString());
        groupRep.setName("test_group");
        List<GroupRepresentation> groups = List.of(groupRep);
        when(userResource.groups()).thenReturn(groups);

        UserResponse expectedResponse = new UserResponse(
                "test",
                "test",
                "test@test.com",
                List.of("test_role"),
                List.of("test_group")
        );

        UserResponse resultResponse = userService.getUserById(userId);

        assertThat(resultResponse).isEqualTo(expectedResponse);

    }

    @Test
    void getUserById_ValidException(){

        when(keycloakClient.realm(any())).thenThrow(new RuntimeException());

        assertThrows(BackendResourcesException.class, () ->
            userService.getUserById(UUID.randomUUID())
        );
    }

}
