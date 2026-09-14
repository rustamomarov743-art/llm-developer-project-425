package ru.hexlet.llm.developer425.core;

import tech.ydb.auth.iam.CloudAuthHelper;
import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.query.QueryClient;
import tech.ydb.query.tools.QueryReader;
import tech.ydb.query.tools.SessionRetryContext;
import tech.ydb.table.query.Params;

import java.util.Objects;

public final class Ydb {

    private static final String YDB_ENDPOINT = "YDB_ENDPOINT";
    private static final String YDB_DATABASE = "YDB_DATABASE";

    private Ydb() {
    }

    private static final class Holder {

        private static final SessionRetryContext RETRY_CTX = build();

        private static SessionRetryContext build() {
            String endPoint = PropertySource.get(YDB_ENDPOINT);
            String database = PropertySource.get(YDB_DATABASE);
            String connectionString = endPoint  + database;
            GrpcTransport transport = GrpcTransport.forConnectionString(connectionString)
                    .withAuthProvider(CloudAuthHelper.getMetadataAuthProvider())
                    .build();
            QueryClient client = QueryClient.newClient(transport).build();
            return SessionRetryContext.create(client).build();
        }
    }


    public static void execute(String yql, Params params) {
        Holder.RETRY_CTX
                .supplyResult(session -> QueryReader.readFrom(
                        session.createQuery(yql, TxMode.SERIALIZABLE_RW, params)))
                .join()
                .getStatus()
                .expectSuccess("Запрос к YDB не выполнен");
    }

    public static QueryReader read(String yql) {
        return Holder.RETRY_CTX
                .supplyResult(session -> QueryReader.readFrom(
                        session.createQuery(yql, TxMode.SNAPSHOT_RO)))
                .join()
                .getValue();
    }

    public static QueryReader read(String yql, Params params) {
        return Holder.RETRY_CTX
                .supplyResult(session -> QueryReader.readFrom(
                        session.createQuery(yql, TxMode.SNAPSHOT_RO, Objects.requireNonNull(params, "params is null"))))
                .join()
                .getValue();
    }

    public static void executeSchema(String ddl) {
        Holder.RETRY_CTX
                .supplyResult(session -> QueryReader.readFrom(
                        session.createQuery(ddl, TxMode.NONE, Params.empty())))
                .join()
                .getStatus()
                .expectSuccess("Схемная операция в YDB не выполнена");
    }
}
