package liquibase.change.ext;

import liquibase.Liquibase;
import liquibase.change.ext.s3.change.S3ClientDelegator;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.ChangeLogParseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.MigrationFailedException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3FileChangeTest {

    private S3Client s3Client;
    private Connection connection;

    @BeforeEach
    public void setUp() throws Exception {
        s3Client = mock(S3Client.class);

        Field field = S3ClientDelegator.class.getDeclaredField("s3Client");
        field.setAccessible(true);
        field.set(null, s3Client);

        connection = DriverManager.getConnection("jdbc:h2:mem:testdb");
    }

    @AfterEach
    public void tearDown() throws Exception {
        connection.close();
    }

    @Test
    void shouldApplyChangeSet() throws Exception {
        GetObjectRequest requestToS3 = GetObjectRequest.builder()
                .bucket("miron-test")
                .key("data/dummy.sql")
                .build();

        when(s3Client.getObject(requestToS3)).thenReturn(createInputStream("/s3/dummy.sql"));

        Liquibase liquibase = createLiquibase("liquibase/changelog.valid.sql.xml");

        liquibase.update((String) null);

        try (Statement stm = connection.createStatement()) {
            ResultSet rs = stm.executeQuery("SELECT * FROM BADGE");

            assertTrue(rs.next());

            assertEquals("Test title", rs.getString("TITLE"));
            assertEquals("Test description", rs.getString("DESCRIPTION"));
            assertEquals(12345, rs.getInt("SCORE"));
        }

        verify(s3Client).getObject(requestToS3);
    }

    @Test
    void shouldNotApplyChangeSetBecauseSqlInvalid() throws Exception {
        GetObjectRequest requestToS3 = GetObjectRequest.builder()
                .bucket("miron-test")
                .key("incorrect.sql")
                .build();

        when(s3Client.getObject(requestToS3)).thenReturn(createInputStream("/s3/incorrect.sql"));

        Liquibase liquibase = createLiquibase("liquibase/changelog.invalid.sql.xml");

        assertThrows(MigrationFailedException.class, () -> liquibase.update((String) null));

        verify(s3Client).getObject(requestToS3);
    }

    @Test
    void shouldNotApplyChangeSetBecausePathContainsOnlyBucket() throws LiquibaseException {
        Liquibase liquibase = createLiquibase("liquibase/changelog.only.bucket.xml");

        assertThrows(ChangeLogParseException.class, () -> liquibase.update((String) null));
    }

    private Liquibase createLiquibase(String s) throws LiquibaseException {
        return new Liquibase(s,
                new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
    }

    private ResponseInputStream<GetObjectResponse> createInputStream(String sqlPath) {
        InputStream stream = getClass().getResourceAsStream(sqlPath);
        return new ResponseInputStream<>(GetObjectResponse.builder().build(),
                AbortableInputStream.create(stream));
    }

}