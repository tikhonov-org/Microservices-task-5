package com.itm.space.backendresources;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;

import javax.ws.rs.core.Response;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserControllerIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private Keycloak keycloakClient;

    @MockBean
    private UserMapper userMapper;

    @Test
    @WithMockUser(roles = "MODERATOR")
    void create_ReturnsStatusCreated() throws Exception {

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        Response mockResponse = mock(Response.class);

        when(keycloakClient.realm(any())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(mockResponse.getLocation()).thenReturn(URI.create("/users/123"));

        UserRequest request = new UserRequest(
                "test",
                "test@example.com",
                "test",
                "test",
                "test"
        );

        mvc.perform(requestWithContent(post("/api/users"), request))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void create_ReturnsStatusBadRequest() throws Exception {

        mvc.perform(requestWithContent(post("/api/users"),
                        new UserRequest(
                                "",
                                "test@example.com",
                                "test",
                                "test",
                                "test"
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_ReturnsStatusForbidden() throws Exception {

        mvc.perform(requestWithContent(post("/api/users"),
                        new UserRequest(
                                "test",
                                "test@example.com",
                                "test",
                                "test",
                                "test"
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_ReturnsStatusUnauthorized() throws Exception {

        mvc.perform(requestWithContent(post("/api/users"),
                        new UserRequest(
                                "test",
                                "test@example.com",
                                "test",
                                "test",
                                "test"
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void getUserById_ReturnsUserResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(userId.toString());
        userRepresentation.setUsername("test");

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);

        when(keycloakClient.realm(any())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(any())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);

        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);

        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.getAll()).thenReturn(new MappingsRepresentation() {
            {
                setRealmMappings(List.of(
                        new RoleRepresentation("test_role", null, false)
                ));
            }
        });

        when(userResource.groups()).thenReturn(List.of(new GroupRepresentation()));

        UserResponse expectedResponse = new UserResponse(
                "firstName",
                "lastName",
                "test@example.com",
                List.of("test_role"),
                List.of("test_group")
        );

        when(userMapper.userRepresentationToUserResponse(any(), any(), any()))
                .thenReturn(expectedResponse);

        mvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("firstName"))
                .andExpect(jsonPath("$.lastName").value("lastName"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("test_role"))
                .andExpect(jsonPath("$.groups[0]").value("test_group"));
    }

    @Test
    @WithMockUser(username = "tester", roles = "MODERATOR")
    void hello_ShouldReturnUsername() throws Exception {
        mvc.perform(get("/api/users/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("tester"));
    }

}
