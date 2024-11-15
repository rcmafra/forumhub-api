package com.raul.forumhub.topic.integration;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.raul.forumhub.topic.client.UserClientRequest;
import com.raul.forumhub.topic.domain.Status;
import com.raul.forumhub.topic.domain.Topic;
import com.raul.forumhub.topic.dto.request.TopicCreateDTO;
import com.raul.forumhub.topic.dto.request.TopicUpdateDTO;
import com.raul.forumhub.topic.exception.RestClientException;
import com.raul.forumhub.topic.repository.*;
import com.raul.forumhub.topic.utility.TestsHelper;
import org.junit.jupiter.api.*;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@ActiveProfiles(value = "test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(ClassOrderer.ClassName.class)
@Order(2)
public class TopicControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TopicRepository topicRepository;

    @Autowired
    AnswerRepository answerRepository;

    @Autowired
    AuthorRepository authorRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    ProfileRepository profileRepository;

    @MockBean
    ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    UserClientRequest userClientRequest;

    private static final Jwt jwt;

    private static boolean hasBeenInitialized = false;

    static {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("user_id", "1")
                .build();
    }

    @BeforeEach
    void setup() {
        if (!hasBeenInitialized) {
            this.profileRepository.saveAll(TestsHelper.ProfileHelper.profileList());
            this.authorRepository.saveAll(TestsHelper.AuthorHelper.authorList());
            this.courseRepository.saveAll(TestsHelper.CourseHelper.courseList());
            this.topicRepository.saveAll(TestsHelper.TopicHelper.topicList());
            this.answerRepository.saveAll(TestsHelper.AnswerHelper.answerList());
            hasBeenInitialized = true;
        }
    }


    @Order(1)
    @DisplayName("Should fail with status code 401 when create topic if user unauthenticated")
    @Test
    void shouldFailToCreateTopicIfUnauthenticated() throws Exception {
        final TopicCreateDTO topicCreateDTO = new TopicCreateDTO("Dúvida na utilização do Feign Client",
                "Como utilizar o Feign Client para integração do serviço x?",
                1L);

        BDDMockito.given(this.userClientRequest.getUserById(1L)).
                willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(post("/api-forum/v1/forumhub/topics/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicCreateDTO)))
                .andExpect(status().isUnauthorized());

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(3, this.topicRepository.findAll().size())
        );


    }

    @Order(2)
    @DisplayName("Should fail with status code 400 if title property is sent empty when create topic")
    @Test
    void shouldFailIfTitlePropertyIsEmptyWhenCreateTopic() throws Exception {
        final TopicCreateDTO topicCreateDTO = new TopicCreateDTO("",
                "Como utilizar o Feign Client para integração do serviço x?",
                1L);


        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(post("/api-forum/v1/forumhub/topics/create")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicCreateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("O título não pode ser vazio")));

        Topic topic = this.topicRepository.findById(1L).orElseThrow();

        Assertions.assertAll(
                () -> assertEquals("Dúvida na utilização do Feign Client", topic.getTitle()),
                () -> assertEquals("Como utilizar o Feign Client para integração do serviço x?", topic.getQuestion()),
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(3, this.topicRepository.findAll().size())
        );

    }

    @Order(3)
    @DisplayName("Should fail with status code 400 if question property is sent empty when create topic")
    @Test
    void shouldFailIfQuestionPropertyIsEmptyWhenCreateTopic() throws Exception {
        final TopicCreateDTO topicCreateDTO = new TopicCreateDTO("Dúvida na utilização do Feign Client",
                "",
                1L);

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(post("/api-forum/v1/forumhub/topics/create")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicCreateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("A pergunta não pode ser vazia")));

        Topic topic = this.topicRepository.findById(1L).orElseThrow();

        Assertions.assertAll(
                () -> assertEquals("Dúvida na utilização do Feign Client", topic.getTitle()),
                () -> assertEquals("Como utilizar o Feign Client para integração do serviço x?", topic.getQuestion()),
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(3, this.topicRepository.findAll().size())
        );

    }

    /*In a request perform by a client, this scenario returns 400 bad request due
    to the different type of exception thrown. This is because in production
    is oracle database is used, but in tests h2 database is used.*/
    @Order(4)
    @DisplayName("Should fail with status code 500 when create topic if the title " +
            "property is greater than 150 chars")
    @Test
    void shouldFailToCreateTopicIfTitlePropertyExceedsLimit() throws Exception {
        final TopicCreateDTO topicCreateDTO = new TopicCreateDTO(
                "Qual é a diferença entre o Feign Client, RestTemplate e o WebClient no " +
                        "Spring Framework e em que situações é mais adequado utilizá-los durante a " +
                        "integração de um serviço?",
                "Diferença entre o Feign Client, RestTemplate e WebClient",
                1L);

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(post("/api-forum/v1/forumhub/topics/create")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicCreateDTO)))
                .andExpect(status().isInternalServerError());

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(3, this.topicRepository.findAll().size())
        );


    }

    @Order(5)
    @DisplayName("Should fail with status code 404 when create topic if the course not exists")
    @Test
    void shouldFailToCreateTopicIfCourseNotExists() throws Exception {
        final TopicCreateDTO topicCreateDTO = new TopicCreateDTO("Dúvida na utilização do Feign Client",
                "Como utilizar o Feign Client para integração do serviço x?",
                4L);

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(post("/api-forum/v1/forumhub/topics/create")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicCreateDTO)))
                .andExpect(status().isNotFound());

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(3, this.topicRepository.findAll().size())
        );


    }


    @Order(6)
    @DisplayName("Should fail with status code 404 when create topic if the user service " +
            "return 404 not found status code")
    @Test
    void shouldFailToCreateTopicIfUserServiceReturn404StatusCode() throws Exception {
        final TopicCreateDTO topicCreateDTO = new TopicCreateDTO("Dúvida na utilização do Feign Client",
                "Como utilizar o Feign Client para integração do serviço x?",
                1L);

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willThrow(new RestClientException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        this.mockMvc.perform(post("/api-forum/v1/forumhub/topics/create")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicCreateDTO)))
                .andExpectAll(status().isNotFound());


        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(3, this.topicRepository.findAll().size())
        );


    }

    @Order(7)
    @DisplayName("Should create topic with success if user is authenticated and " +
            "previous premisses are adequate")
    @Test
    void shouldCreateTopicWithSuccessIfAuthenticated() throws Exception {
        final TopicCreateDTO topicCreateDTO = new TopicCreateDTO("Dúvida na utilização do Feign Client",
                "Como utilizar o Feign Client para integração do serviço x?",
                1L);

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(post("/api-forum/v1/forumhub/topics/create")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicCreateDTO)))
                .andExpectAll(status().isCreated(),
                        content().string("{\"message\":\"HttpStatusCode OK\"}")

                );

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );


    }


    @Order(8)
    @DisplayName("Should fail with status code 404 when request the specified topic if not exists")
    @Test
    void shouldFailToRequestTheSpecifiedTopicIfNotExists() throws Exception {
        this.mockMvc.perform(get("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isNotFound());

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );


    }

    @Order(9)
    @DisplayName("Should return all topics unsorted with successful")
    @Test
    void shouldReturnAllTopicsUnsortedWithSuccessful() throws Exception {
        this.mockMvc.perform(get("/api-forum/v1/forumhub/topics/listAll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..getTopicDTOList.length()", is(4)))
                .andExpect(jsonPath("$..page.[?(@.size == 10)]").exists())
                .andExpect(jsonPath("$..page.[?(@.totalElements == 4)]").exists())
                .andExpect(jsonPath("$..page.[?(@.totalPages == 1)]").exists());

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );


    }

    @Order(10)
    @DisplayName("Should return all topics sorted descendants by created date with successful")
    @Test
    void shouldReturnAllTopicsSortedDescendantByCreateDateWithSuccessful() throws Exception {
        this.mockMvc.perform(get("/api-forum/v1/forumhub/topics/listAll")
                        .queryParam("sort", "createdAt,desc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..getTopicDTOList[0].[?(@.id == 5)]").exists())
                .andExpect(jsonPath("$..getTopicDTOList[1].[?(@.id == 3)]").exists())
                .andExpect(jsonPath("$..getTopicDTOList[2].[?(@.id == 1)]").exists())
                .andExpect(jsonPath("$..getTopicDTOList[3].[?(@.id == 2)]").exists())
                .andExpect(jsonPath("$..getTopicDTOList.length()", is(4)))
                .andExpect(jsonPath("$..page.[?(@.size == 10)]").exists())
                .andExpect(jsonPath("$..page.[?(@.totalElements == 4)]").exists())
                .andExpect(jsonPath("$..page.[?(@.totalPages == 1)]").exists());

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );


    }

    @Order(11)
    @DisplayName("Should return only two topics sorted in ascendant by status with successful")
    @Test
    void shouldReturnTwoTopicsSortedAscendantByStatusWithSuccessful() throws Exception {
        this.mockMvc.perform(get("/api-forum/v1/forumhub/topics/listAll")
                        .queryParam("size", "2")
                        .queryParam("sort", "status,asc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..getTopicDTOList[0].[?(@.id == 3)]").exists())
                .andExpect(jsonPath("$..getTopicDTOList[0].[?(@.status == \"SOLVED\")]").exists())
                .andExpect(jsonPath("$..getTopicDTOList[1].[?(@.id == 5)]").exists())
                .andExpect(jsonPath("$..getTopicDTOList[1].[?(@.status == \"UNSOLVED\")]").exists())
                .andExpect(jsonPath("$..page.[?(@.number == 0)]").exists())
                .andExpect(jsonPath("$..getTopicDTOList.length()", is(2)))
                .andExpect(jsonPath("$..page.[?(@.size == 2)]").exists())
                .andExpect(jsonPath("$..page.[?(@.totalElements == 4)]").exists())
                .andExpect(jsonPath("$..page.[?(@.totalPages == 2)]").exists());

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );


    }


    @Order(12)
    @DisplayName("Should return all topics sorted ascendants by title with successful")
    @Test
    void shouldReturnAllTopicsSortedAscendantByTitleWithSuccessful() throws Exception {
        this.mockMvc.perform(get("/api-forum/v1/forumhub/topics/listAll")
                        .queryParam("sort", "title,asc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..getTopicDTOList[0].[?(@.id == 3)]").exists())
                .andExpect(jsonPath("$..getTopicDTOList[1].[?(@.id == 1)]").exists())
                .andExpect(jsonPath("$..getTopicDTOList[2].[?(@.id == 5)]").exists())
                .andExpect(jsonPath("$..getTopicDTOList[3].[?(@.id == 2)]").exists())
                .andExpect(jsonPath("$..page.[?(@.number == 0)]").exists())
                .andExpect(jsonPath("$..getTopicDTOList.length()", is(4)))
                .andExpect(jsonPath("$..page.[?(@.size == 10)]").exists())
                .andExpect(jsonPath("$..page.[?(@.totalElements == 4)]").exists())
                .andExpect(jsonPath("$..page.[?(@.totalPages == 1)]").exists());

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );


    }

    @Order(13)
    @DisplayName("Should fail with status code 400 when attempt get topic if topic_id property " +
            "of query param is sent empty")
    @Test
    void shouldFailIfTopicIdPropertyOfQueryParamIsEmptyWhenGetTopic() throws Exception {
        this.mockMvc.perform(get("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest());

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }


    @Order(14)
    @DisplayName("Should return the specified topic with successful if exists")
    @Test
    void shouldReturnTheSpecifiedTopicWithSuccessful() throws Exception {
        this.mockMvc.perform(get("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.id == 1)]").exists())
                .andExpect(jsonPath("$.title", is("Dúvida na utilização do Feign Client")));

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );


    }

    @Order(15)
    @DisplayName("Should fail with status code 403 if user authenticated hasn't authority 'topic:edit'" +
            "when edit topic")
    @Test
    void shouldFailIfUserHasNotSuitableAuthorityWhenEditTopic() throws Exception {
        final TopicUpdateDTO topicUpdateDTO = new TopicUpdateDTO(
                new Topic("Dúvida quanto a utilização do Elasticsearch",
                        "Como posso integrar minha API com o Elasticsearch para monitoração?",
                        Status.UNSOLVED), 1L
        );

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(put("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "1")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicUpdateDTO)))
                .andExpect(status().isForbidden());

        Topic topic = this.topicRepository.findById(1L).orElseThrow();

        Assertions.assertAll(
                () -> assertEquals("Dúvida na utilização do Feign Client", topic.getTitle()),
                () -> assertEquals("Como utilizar o Feign Client para integração do serviço x?", topic.getQuestion()),
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }

    @Order(16)
    @DisplayName("Should fail with status code 400 if title property is sent empty when edit topic")
    @Test
    void shouldFailIfTitlePropertyIsEmptyWhenEditTopic() throws Exception {
        final TopicUpdateDTO topicUpdateDTO = new TopicUpdateDTO(
                new Topic("",
                        "Como posso integrar minha API com o Elasticsearch para monitoração?",
                        Status.UNSOLVED), 1L
        );

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(put("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "1")
                        .with(jwt().jwt(jwt)
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:edit")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicUpdateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("O título não pode ser vazio")));

        Topic topic = this.topicRepository.findById(1L).orElseThrow();

        Assertions.assertAll(
                () -> assertEquals("Dúvida na utilização do Feign Client", topic.getTitle()),
                () -> assertEquals("Como utilizar o Feign Client para integração do serviço x?", topic.getQuestion()),
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }

    @Order(17)
    @DisplayName("Should fail with status code 400 if question property is sent empty when edit topic")
    @Test
    void shouldFailIfQuestionPropertyIsEmptyWhenEditTopic() throws Exception {
        TopicUpdateDTO topicUpdateDTO = new TopicUpdateDTO(
                new Topic("Dúvida quanto a utilização do Elasticsearch",
                        "",
                        Status.UNSOLVED), 1L
        );

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(put("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "1")
                        .with(jwt().jwt(jwt)
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:edit")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicUpdateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("A pergunta não pode ser vazia")));

        Topic topic = this.topicRepository.findById(1L).orElseThrow();

        Assertions.assertAll(
                () -> assertEquals("Dúvida na utilização do Feign Client", topic.getTitle()),
                () -> assertEquals("Como utilizar o Feign Client para integração do serviço x?", topic.getQuestion()),
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }

    @Order(18)
    @DisplayName("Should fail with status code 400 when attempt update topic if topic_id property " +
            "of query param is sent empty")
    @Test
    void shouldFailIfTopicIdPropertyOfQueryParamIsEmptyWhenUpdateTopic() throws Exception {
        TopicUpdateDTO topicUpdateDTO = new TopicUpdateDTO(
                new Topic("Dúvida quanto a utilização do Elasticsearch",
                        "Como posso integrar minha API com o Elasticsearch para monitoração?",
                        Status.UNSOLVED), 1L
        );

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(put("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "")
                        .with(jwt().jwt(jwt)
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:edit")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicUpdateDTO)))
                .andExpect(status().isBadRequest());

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }

    @Order(19)
    @DisplayName("Should fail with status code 404 when update topic if the course not exists")
    @Test
    void shouldFailToEditTopicIfCourseNotExists() throws Exception {
        final TopicUpdateDTO topicUpdateDTO = new TopicUpdateDTO(
                new Topic("Dúvida quanto a utilização do Elasticsearch",
                        "Como posso integrar minha API com o Elasticsearch para monitoração?",
                        Status.UNSOLVED), 4L
        );

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(put("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "1")
                        .with(jwt().jwt(jwt)
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:edit")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicUpdateDTO)))
                .andExpect(status().isNotFound());

        Topic topic = this.topicRepository.findById(1L).orElseThrow();

        Assertions.assertAll(
                () -> assertEquals("Dúvida na utilização do Feign Client", topic.getTitle()),
                () -> assertEquals("Como utilizar o Feign Client para integração do serviço x?", topic.getQuestion()),
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );


    }


    @Order(20)
    @DisplayName("Should fail with status code 404 when edit topic if the user service return " +
            "404 not found status code")
    @Test
    void shouldFailToEditTopicIfUserServiceReturn404StatusCode() throws Exception {
        final TopicUpdateDTO topicUpdateDTO = new TopicUpdateDTO(
                new Topic("Dúvida quanto a utilização do Elasticsearch",
                        "Como posso integrar minha API com o Elasticsearch para monitoração?",
                        Status.UNSOLVED), 1L
        );

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willThrow(new RestClientException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        this.mockMvc.perform(put("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "1")
                        .with(jwt().jwt(jwt)
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:edit")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicUpdateDTO)))
                .andExpect(status().isNotFound());

        Topic topic = this.topicRepository.findById(1L).orElseThrow();

        Assertions.assertAll(
                () -> assertEquals("Dúvida na utilização do Feign Client", topic.getTitle()),
                () -> assertEquals("Como utilizar o Feign Client para integração do serviço x?", topic.getQuestion()),
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }


    @Order(21)
    @DisplayName("Should fail with status code 418 if basic user attempt edit topic of other author")
    @Test
    void shouldFailIfBasicUserAttemptEditTopicOfOtherAuthor() throws Exception {
        final TopicUpdateDTO topicUpdateDTO = new TopicUpdateDTO(
                new Topic("Dúvida quanto a utilização do Elasticsearch",
                        "Como posso integrar minha API com o Elasticsearch para monitoração?",
                        Status.UNSOLVED), 1L
        );

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(put("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "2")
                        .with(jwt().jwt(jwt)
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:edit")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicUpdateDTO)))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.detail", is("Privilégio insuficiente")));

        Topic topic = this.topicRepository.findById(2L).orElseThrow();

        Assertions.assertAll(
                () -> assertEquals("Dúvida na utilização do OpenShift", topic.getTitle()),
                () -> assertEquals("Como utilizar o Rosa/OpenShift para implantação do serviço x?", topic.getQuestion()),
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }

    @Order(22)
    @DisplayName("Should fail with status code 422 when attempt edit a topic of unknown author")
    @Test
    void shouldFailWhenAttemptEditTopicOfUnknownAuthor() throws Exception {
        final TopicUpdateDTO topicUpdateDTO = new TopicUpdateDTO(
                new Topic("Dúvida quanto a utilização do Elasticsearch",
                        "Como posso integrar minha API com o Elasticsearch para monitoração?",
                        Status.SOLVED), 1L
        );

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(2));

        this.mockMvc.perform(put("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "3")
                        .with(jwt().jwt(jwt)
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:edit")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicUpdateDTO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", is(
                        "O tópico pertence a um autor inexistente," +
                                " ele não pode ser editado"
                )));

        Topic topic = this.topicRepository.findById(3L).orElseThrow();

        Assertions.assertAll(
                () -> assertEquals("Dúvida em relação ao teste end-to-end", topic.getTitle()),
                () -> assertEquals("Quais as boas práticas na execução dos testes end-to-end?", topic.getQuestion()),
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );


    }

    @Order(23)
    @DisplayName("Topic author should be able edit specified topic if authenticated, " +
            "has authority 'topic:edit' and previous premisses are adequate")
    @Test
    void topicAuthorShouldEditSpecifiedTopicWithSuccessIfHasSuitableAuthority() throws Exception {
        final TopicUpdateDTO topicUpdateDTO = new TopicUpdateDTO(
                new Topic("Dúvida na utilização do WebClient",
                        "Como utilizar o WebClient para integração do serviço x?",
                        Status.UNSOLVED), 1L
        );

        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(put("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "1")
                        .with(jwt().jwt(jwt)
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:edit")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicUpdateDTO)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"HttpStatusCode OK\"}"));


        Topic topic = this.topicRepository.findById(1L).orElseThrow();

        Assertions.assertAll(
                () -> assertEquals("Dúvida na utilização do WebClient", topic.getTitle()),
                () -> assertEquals("Como utilizar o WebClient para integração do serviço x?", topic.getQuestion()),
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }

    @Order(24)
    @DisplayName("User ADM should be able edit topic of other author if authenticated, " +
            "has authority 'topic:edit' and previous premisses are adequate")
    @Test
    void userADMShouldEditTopicOfOtherAuthorWithSuccessIfHasSuitableAuthority() throws Exception {
        final TopicUpdateDTO topicUpdateDTO = new TopicUpdateDTO(
                new Topic("Dúvida na utilização do RestTemplate",
                        "Como utilizar o RestTemplate para integração do serviço x?",
                        Status.UNSOLVED), 1L
        );

        BDDMockito.given(this.userClientRequest.getUserById(3L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(2));

        this.mockMvc.perform(put("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "1")
                        .with(jwt().jwt(jwt -> jwt.claim("user_id", "3"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:edit")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicUpdateDTO)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"HttpStatusCode OK\"}"));


        Topic topic = this.topicRepository.findById(1L).orElseThrow();

        Assertions.assertAll(
                () -> assertEquals("Dúvida na utilização do RestTemplate", topic.getTitle()),
                () -> assertEquals("Como utilizar o RestTemplate para integração do serviço x?", topic.getQuestion()),
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }

    @Order(25)
    @DisplayName("User MOD should be able edit topic of other author if authenticated, " +
            "has authority 'topic:edit' and previous premisses are adequate")
    @Test
    void userMODShouldEditTopicOfOtherAuthorWithSuccessIfHasSuitableAuthority() throws Exception {
        final TopicUpdateDTO topicUpdateDTO = new TopicUpdateDTO(
                new Topic("Dúvida na utilização da API de validação do Spring",
                        "Quais são as anotações da API de validação do Spring?",
                        Status.UNSOLVED), 1L
        );

        BDDMockito.given(this.userClientRequest.getUserById(2L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(1));

        this.mockMvc.perform(put("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "1")
                        .with(jwt().jwt(jwt -> jwt.claim("user_id", "2"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:edit")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(new ObjectMapper()
                                .writeValueAsString(topicUpdateDTO)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"HttpStatusCode OK\"}"));


        Topic topic = this.topicRepository.findById(1L).orElseThrow();

        Assertions.assertAll(
                () -> assertEquals("Dúvida na utilização da API de validação do Spring", topic.getTitle()),
                () -> assertEquals("Quais são as anotações da API de validação do Spring?", topic.getQuestion()),
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }


    @Order(26)
    @DisplayName("Should fail with status code 403 if user authenticated hasn't authority 'topic:delete'" +
            " when delete topic")
    @Test
    void shouldFailIfUserHasNotSuitableAuthorityWhenDeleteTopic() throws Exception {
        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn((TestsHelper.AuthorHelper.authorList().get(0)));

        this.mockMvc.perform(delete("/api-forum/v1/forumhub/topics/delete")
                        .queryParam("topic_id", "1")
                        .with(jwt().jwt(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isForbidden());

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }

    @Order(27)
    @DisplayName("Should fail with status code 400 when attempt delete topic if topic_id property " +
            "of query param is sent empty")
    @Test
    void shouldFailIfTopicIdPropertyOfQueryParamIsEmptyWhenDeleteTopic() throws Exception {
        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(delete("/api-forum/v1/forumhub/topics")
                        .queryParam("topic_id", "")
                        .with(jwt().jwt(jwt)
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:delete")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest());

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }

    @Order(28)
    @DisplayName("Should fail with status code 404 when delete topic if the user service " +
            "return 404 not found status code")
    @Test
    void shouldFailToDeleteTopicIfUserServiceReturn404StatusCode() throws Exception {
        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willThrow(new RestClientException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        this.mockMvc.perform(delete("/api-forum/v1/forumhub/topics/delete")
                        .queryParam("topic_id", "1")
                        .with(jwt().jwt(jwt)
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:delete")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isNotFound());

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }

    @Order(29)
    @DisplayName("Should fail with status code 418 if basic user attempt delete topic of other author")
    @Test
    void shouldFailIfBasicUserAttemptDeleteTopicOfOtherAuthor() throws Exception {
        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(delete("/api-forum/v1/forumhub/topics/delete")
                        .queryParam("topic_id", "2")
                        .with(jwt().jwt(jwt)
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:delete")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.detail", is("Privilégio insuficiente")));

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(5, this.answerRepository.findAll().size()),
                () -> assertEquals(4, this.topicRepository.findAll().size())
        );

    }

    @Order(30)
    @DisplayName("Topic author should be able delete specified topic if authenticated, " +
            "has authority 'topic:delete' and previous premisses are adequate")
    @Test
    void topicAuthorShouldDeleteSpecifiedTopicWithSuccessIfHasSuitableAuthority() throws Exception {
        BDDMockito.given(this.userClientRequest.getUserById(1L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(0));

        this.mockMvc.perform(delete("/api-forum/v1/forumhub/topics/delete")
                        .queryParam("topic_id", "1")
                        .with(jwt().jwt(jwt)
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:delete")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"HttpStatusCode OK\"}"));

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(2, this.answerRepository.findAll().size()),
                () -> assertEquals(3, this.topicRepository.findAll().size())
        );

    }

    @Order(31)
    @DisplayName("User ADM should be able delete topic of other author if authenticated, " +
            "has authority 'topic:delete' and previous premisses are adequate")
    @Test
    void userADMShouldDeleteTopicOfOtherAuthorWithSuccessIfHasSuitableAuthority() throws Exception {
        BDDMockito.given(this.userClientRequest.getUserById(3L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(2));

        this.mockMvc.perform(delete("/api-forum/v1/forumhub/topics/delete")
                        .queryParam("topic_id", "2")
                        .with(jwt().jwt(jwt -> jwt.claim("user_id", "3"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:delete")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"HttpStatusCode OK\"}"));

        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(1, this.answerRepository.findAll().size()),
                () -> assertEquals(2, this.topicRepository.findAll().size())
        );

    }

    @Order(32)
    @DisplayName("User MOD should be able delete topic of other author if authenticated, " +
            "has authority 'topic:delete' and previous premisses are adequate")
    @Test
    void userMODShouldDeleteTopicOfOtherAuthorWithSuccessIfHasSuitableAuthority() throws Exception {
        BDDMockito.given(this.userClientRequest.getUserById(2L))
                .willReturn(TestsHelper.AuthorHelper.authorList().get(1));

        this.mockMvc.perform(delete("/api-forum/v1/forumhub/topics/delete")
                        .queryParam("topic_id", "3")
                        .with(jwt().jwt(jwt -> jwt.claim("user_id", "2"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_topic:delete")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"HttpStatusCode OK\"}"));


        Assertions.assertAll(
                () -> assertEquals(3, this.profileRepository.findAll().size()),
                () -> assertEquals(4, this.authorRepository.findAll().size()),
                () -> assertEquals(3, this.courseRepository.findAll().size()),
                () -> assertEquals(0, this.answerRepository.findAll().size()),
                () -> assertEquals(1, this.topicRepository.findAll().size())
        );

    }


}