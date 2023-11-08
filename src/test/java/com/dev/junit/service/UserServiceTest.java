package com.dev.junit.service;

import com.dev.junit.TestBase;
import com.dev.junit.dao.UserDao;
import com.dev.junit.dto.User;
import com.dev.junit.extension.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.collection.IsEmptyCollection.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JUnit 5. 14. Extension Model. Теория
 */

@Tag("fast")
@Tag("user")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class) // @TestMethodOrder - лучше его не использовать вообще.
@ExtendWith({
        UserServiceParamResolver.class,
        PostProcessingExtension.class,
        MockitoExtension.class
        //ThrowableExtension.class
        //ConditionalExtension.class
        //GlobalExtension.class
})
class UserServiceTest extends TestBase {

    private static final User IVAN = User.of(1, "Ivan", "123");
    private static final User PETR = User.of(2, "Petr", "111");

    @Captor
    private ArgumentCaptor<Integer> argumentCaptor;
    @Mock(lenient = true)
    private UserDao userDao;
    @InjectMocks
    private UserService userService;

    UserServiceTest(TestInfo testInfo) {
        System.out.println();
    }

    @BeforeAll
    void init() {
        System.out.println("Before All: " + this);
    }

    @BeforeEach
    void prepare() {
        System.out.println("Before each: " + this);
        //lenient().when(userDao.delete(IVAN.getId())).thenReturn(true);

        doReturn(true).when(userDao).delete(IVAN.getId());
        //this.userDao = Mockito.mock(UserDao.class);

        //this.userDao = Mockito.spy(new UserDao());
        //this.userService = new UserService(userDao);
    }

    @Test
    void throwExceptionIfDatabaseIsNotAvailable() {
        doThrow(RuntimeException.class).when(userDao).delete(IVAN.getId());

        assertThrows(RuntimeException.class, () -> userService.delete(IVAN.getId()));
    }

    @Test
    void shouldDeleteExistedUser() {
        userService.add(IVAN);
        //Mockito.doReturn(true).when(userDao).delete(IVAN.getId());
        //Mockito.doReturn(true).when(userDao).delete(Mockito.any());

        /*Mockito.when(userDao.delete(IVAN.getId()))
                .thenReturn(true)
                .thenReturn(false);*/

        var deleteResult = userService.delete(IVAN.getId());
        System.out.println(userService.delete(IVAN.getId()));
        System.out.println(userService.delete(IVAN.getId()));

        //var argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(userDao, Mockito.times(3)).delete(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(IVAN.getId());

        //Mockito.reset(userDao);

        assertThat(deleteResult).isTrue();
    }

    @Test
    @DisplayName("users will be empty if no user added")
    void usersEmptyIfNoUserAdded(UserService userService) throws IOException {
        if (true) {
            throw new RuntimeException();
        }
        System.out.println("Test 1: " + this);
        var users = userService.getAll();

        MatcherAssert.assertThat(users, empty());

        //assertTrue(users.isEmpty());
        assertTrue(users.isEmpty(), () -> "User list should be empty");

        // input -> [box == func] -> actual output
    }

    @Test
    void usersSizeIfUserAdded() {
        System.out.println("Test 2: " + this);
        userService.add(IVAN);
        userService.add(PETR);

        var users = userService.getAll();

        assertThat(users).hasSize(2);
        //assertEquals(2, users.size());
    }

    @Test
    void usersConvertedToMapById() {
        userService.add(IVAN, PETR);

        Map<Integer, User> users = userService.getAllConvertedById();

        MatcherAssert.assertThat(users, IsMapContaining.hasKey(IVAN.getId()));
        assertAll(
                () -> assertThat(users).containsKeys(IVAN.getId(), PETR.getId()),
                () -> assertThat(users).containsValues(IVAN, PETR)
        );
    }

    @AfterEach
    void deleteDataFromDatabase() {
        System.out.println("After each: " + this);
    }

    @AfterAll
    void closeConnectionPool() {
        System.out.println("After All: " + this);
    }

    @Nested
    @Tag("login")
    @DisplayName("test user login functionality")
    class LoginTest {

        @Test
        @Disabled("flaky, need to see")
            // Disabled - позволяет нам не запускать наш тест в след. раз.
        void loginSuccessIfUserExists() {
            userService.add(IVAN);

            Optional<User> maybeUser = userService.login(IVAN.getUsername(), IVAN.getPassword());

            assertThat(maybeUser).isPresent();
            maybeUser.ifPresent(user -> assertThat(user).isEqualTo(IVAN));
            //assertTrue(maybeUser.isPresent());
            //maybeUser.ifPresent(user -> assertEquals(IVAN, user));
        }

        @Test
        void throwExceptionIfUsernameOrPasswordIsNull() {
            assertAll(
                    () -> {
                        var exception = assertThrows(IllegalArgumentException.class, () -> userService.login(null, "dummy"));
                        assertThat(exception.getMessage()).isEqualTo("username or password is null");
                    },
                    () -> assertThrows(IllegalArgumentException.class, () -> userService.login("dummy", null))
            );
        }

        @Test
        void loginFailIfPasswordIsNotCorrect() {
            userService.add(IVAN);

            var maybeUser = userService.login(IVAN.getUsername(), "dummy");

            assertTrue(maybeUser.isEmpty());
        }

        //@Test
        @RepeatedTest(value = 5, name = RepeatedTest.LONG_DISPLAY_NAME)
        // Указывем сколько репетишенов должно быть.
        void loginFailIfUserDoesNotExist() {
            userService.add(IVAN);

            var maybeUser = userService.login("dummy", IVAN.getPassword());

            assertTrue(maybeUser.isEmpty());
        }

        @Test
            //@Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
        void checkLoginFunctionalityPerformance() {
            System.out.println(Thread.currentThread().getName());
            var result = assertTimeoutPreemptively(Duration.ofMillis(200L), () -> {
                System.out.println(Thread.currentThread().getName());
                Thread.sleep(100L);
                return userService.login("dummy", IVAN.getPassword());
            });
        }

        @ParameterizedTest(name = "{arguments} test")
        //@ArgumentsSource()
        //@NullSource // Работает только с одним параметром.
        //@EmptySource // Работает только с одним параметром.
        //@NullAndEmptySource
        //@ValueSource // Работает только с одним параметром.
        /*@ValueSource(strings = {
                "Ivan", "Petr"
        })*/
        //@EnumSource // Используется только тогда когда у нас есть Enum всесто приметивных типов.
        @MethodSource("com.dev.junit.service.UserServiceTest#getArgumentsForLoginTest")
        //@CsvFileSource(resources = "/login-test-data.csv", delimiter = ',', numLinesToSkip = 1)
        /*@CsvSource({
                "Ivan,123",
                "Petr,111"
        })*/
        @DisplayName("login param test")
        void loginParameterizedTest(String username, String password, Optional<User> user) {
            userService.add(IVAN, PETR);

            var maybeUser = userService.login(username, password);
            assertThat(maybeUser).isEqualTo(user);
        }
    }

    static Stream<Arguments> getArgumentsForLoginTest() {
        return Stream.of(
                Arguments.of("Ivan", "123", Optional.of(IVAN)),
                Arguments.of("Petr", "111", Optional.of(PETR)),
                Arguments.of("Petr", "dummy", Optional.empty()),
                Arguments.of("dummy", "123", Optional.empty())
        );
    }
}
