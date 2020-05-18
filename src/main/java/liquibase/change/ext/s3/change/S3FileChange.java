package liquibase.change.ext.s3.change;

import liquibase.change.AbstractSQLChange;
import liquibase.change.ChangeMetaData;
import liquibase.change.DatabaseChange;
import liquibase.change.DatabaseChangeProperty;
import liquibase.changelog.ChangeLogParameters;
import liquibase.database.Database;
import liquibase.exception.SetupException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.util.StreamUtil;
import liquibase.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

@DatabaseChange(name = "s3File",
        description = "The 's3File' tag allows you to specify any sql statements and have it stored on AWS S3 in a " +
                "file. It is useful for complex changes that are not supported through Liquibase's automated refactoring " +
                "tags such as stored procedures.\n" +
                "\n" +
                "The 's3File' tag can also support multiline statements in the same file. Statements can either be " +
                "split using a ; at the end of the last line of the SQL or a go on its own on the line between the " +
                "statements can be used.Multiline SQL statements are also supported and only a ; or go statement " +
                "will finish a statement, a new line is not enough. Files containing a single statement do not " +
                "need to use a ; or go.\n" +
                "\n" +
                "The sql file can also contain comments of either of the following formats:\n" +
                "\n" +
                "A multiline comment that starts with /* and ends with */.\n" +
                "A single line comment starting with <space>--<space> and finishing at the end of the line",
        priority = ChangeMetaData.PRIORITY_DEFAULT)
public class S3FileChange extends AbstractSQLChange {

    private String bucket;
    private String key;

    @Override
    public boolean generateStatementsVolatile(Database database) {
        return false;
    }

    @Override
    public boolean generateRollbackStatementsVolatile(Database database) {
        return false;
    }

    @DatabaseChangeProperty(description = "The S3 bucket of the SQL file to load", requiredForDatabase = "all")
    public String getBucket() {
        return bucket;
    }

    public void setBucket(String fileName) {
        bucket = fileName;
    }

    @DatabaseChangeProperty(description = "The S3 key of the SQL file to load", requiredForDatabase = "all")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * The encoding of the file containing SQL statements
     *
     * @return the encoding
     */
    @DatabaseChangeProperty(exampleValue = "utf8")
    public String getEncoding() {
        return encoding;
    }

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public void finishInitialization() throws SetupException {
        if (bucket == null) {
            throw new SetupException("<s3File> - No bucket specified");
        }
        if (key == null) {
            throw new SetupException("<s3File> - No key specified");
        }
    }

    @Override
    public ValidationErrors validate(Database database) {
        ValidationErrors validationErrors = new ValidationErrors();
        if (StringUtils.trimToNull(getBucket()) == null) {
            validationErrors.addError("'bucket' is required");
        }
        if (StringUtils.trimToNull(getKey()) == null) {
            validationErrors.addError("'key' is required");
        }
        return validationErrors;
    }

    @Override
    public String getConfirmationMessage() {
        return "SQL in file " + bucket + "/" + key + " executed";
    }

    @Override
    public InputStream openSqlStream() {
        if (bucket == null || key == null) {
            return null;
        }

        return S3ClientDelegator.getObject(bucket, key);
    }

    @Override
    @DatabaseChangeProperty(isChangeProperty = false)
    public String getSql() {
        String sql = super.getSql();
        if (sql == null) {
            InputStream sqlStream;
            try {
                sqlStream = openSqlStream();
                if (sqlStream == null) {
                    return null;
                }
                String content = StreamUtil.getStreamContents(sqlStream, encoding);
                if (getChangeSet() != null) {
                    ChangeLogParameters parameters = getChangeSet().getChangeLogParameters();
                    if (parameters != null) {
                        content = parameters.expandExpressions(content, getChangeSet().getChangeLog());
                    }
                }
                return content;
            } catch (IOException e) {
                throw new UnexpectedLiquibaseException(e);
            }
        } else {
            return sql;
        }
    }

    @Override
    public void setSql(String sql) {
        if ((getChangeSet() != null) && (getChangeSet().getChangeLogParameters() != null)) {
            sql = getChangeSet().getChangeLogParameters().expandExpressions(sql, getChangeSet().getChangeLog());
        }
        super.setSql(sql);
    }

}
