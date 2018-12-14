package com.nec.baas.cloudfn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;
import org.slf4j.Logger;

import com.nec.baas.cloudfn.sdk.ApigwResponse;
import com.nec.baas.cloudfn.sdk.ClientContext;
import com.nec.baas.cloudfn.sdk.Context;
import com.nec.baas.core.NbBucketMode;
import com.nec.baas.core.NbCallback;
import com.nec.baas.core.NbErrorInfo;
import com.nec.baas.core.NbListCallback;
import com.nec.baas.core.NbRestExecutor;
import com.nec.baas.core.NbService;
import com.nec.baas.core.internal.NbGenericSessionToken;
import com.nec.baas.core.internal.NbRestExecutorFactory;
import com.nec.baas.core.internal.NbServiceImpl;
import com.nec.baas.generic.internal.NbGenericRestExecutor;
import com.nec.baas.http.NbHttpClient;
import com.nec.baas.json.NbJSONObject;
import com.nec.baas.object.NbObject;
import com.nec.baas.object.internal.NbObjectBucketImpl;
import com.nec.baas.object.internal.NbObjectBucketManagerImpl;

class InfoMotionTest {

    @InjectMocks
    private InfoMotion infomotion;

    @Mock
    Context context;

    @Mock
    Logger logger;

    @Mock
    ClientContext clientContext;

    private static final String BUCKET_NAME = "test_bucket";

    private NbService service = new NbServiceImpl();

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);

        when(context.clientContext()).thenReturn(clientContext);
        when(context.logger()).thenReturn(logger);
        NbServiceImpl.__clearSingleton();
        ((NbServiceImpl) service).initialize("tenantid123", "appId123", "appKey123", "http://unittest/api/1/tenantid/objects/test_bucket",
                new NbGenericSessionToken(), new GenericRestExecutorFactory());
    }

    /**
     * クエリパラメータを使用し、クエリが実行できること<br>
     * レスポンスがDataSourceスキーマに沿った形式で取得できること<br>
     * クエリパラメータ：start指定<br>
     * パスパラメータ：test_bucket
     */
    @Test
    public void testQueryParameterStart() {

        Date baseDate = new Date();
        Long start = baseDate.getTime() - 60 * 60 * 1000;

        when(clientContext.queryParams()).thenReturn(createQueryParams(start, null, null));
        when(clientContext.pathParams()).thenReturn(createPathParams(BUCKET_NAME));
        executeQuery(ApigwResponse.ok().entity(createApigwResponseData().toString()).build(), createSuccessAnswer());
    }

    /**
     * クエリパラメータを使用し、クエリが実行できること<br>
     * レスポンスがDataSourceスキーマに沿った形式で取得できること<br>
     * クエリパラメータ：end指定<br>
     * パスパラメータ：test_bucket
     */
    @Test
    public void testQueryParameterEnd() {

        Date baseDate = new Date();
        Long end = baseDate.getTime();

        when(clientContext.queryParams()).thenReturn(createQueryParams(null, end, null));
        when(clientContext.pathParams()).thenReturn(createPathParams(BUCKET_NAME));
        executeQuery(ApigwResponse.ok().entity(createApigwResponseData().toString()).build(), createSuccessAnswer());
    }

    /**
     * クエリパラメータを使用し、クエリが実行できること<br>
     * レスポンスがDataSourceスキーマに沿った形式で取得できること<br>
     * クエリパラメータ：start, end指定<br>
     * パスパラメータ：test_bucket
     */
    @Test
    public void testQueryParameterStartEnd() {

        Date baseDate = new Date();
        Long start = baseDate.getTime() - 60 * 60 * 1000;
        Long end = baseDate.getTime();

        when(clientContext.queryParams()).thenReturn(createQueryParams(start, end, null));
        when(clientContext.pathParams()).thenReturn(createPathParams(BUCKET_NAME));
        executeQuery(ApigwResponse.ok().entity(createApigwResponseData().toString()).build(), createSuccessAnswer());
    }

    /**
     * クエリパラメータを使用し、クエリが実行できること<br>
     * レスポンスがDataSourceスキーマに沿った形式で取得できること<br>
     * クエリパラメータ：start, end, where指定<br>
     * パスパラメータ：test_bucket
     */
    @Test
    public void testQueryParameterStartEndWhere() {

        Date baseDate = new Date();
        Long start = baseDate.getTime() - 60 * 60 * 1000;
        Long end = baseDate.getTime();

        when(clientContext.queryParams()).thenReturn(createQueryParams(start, end, "{\"name\":\"categoryA\"}"));
        when(clientContext.pathParams()).thenReturn(createPathParams(BUCKET_NAME));
        executeQuery(ApigwResponse.ok().entity(createApigwResponseData().toString()).build(), createSuccessAnswer());
    }

    /**
     * クエリ結果に"ts"キーが含まれている場合は"_ts"キーに格納されること
     */
    @Test
    public void testTsKeyDuplicated() {
        when(clientContext.queryParams()).thenReturn(createQueryParams(null, null, null));
        when(clientContext.pathParams()).thenReturn(createPathParams(BUCKET_NAME));
        executeQuery(ApigwResponse.ok().entity(createApigwResponseDataTsKey().toString()).build(), createSuccessAnswerTsKeyExist());

    }

    /**
     * "_ts"キーが重複した場合は"ts"キーデータで上書きされること
     */
    @Test
    public void testUnderscoreTsKeyDuplicated() {
        when(clientContext.queryParams()).thenReturn(createQueryParams(null, null, null));
        when(clientContext.pathParams()).thenReturn(createPathParams(BUCKET_NAME));
        executeQuery(ApigwResponse.ok().entity(createApigwResponseDataTsKey().toString()).build(), createSuccessAnswerUnderscoreTsValueOverwrite());

    }

    /**
     * クエリパラメータのstart, endに数値以外を設定し、400エラーとなること
     */
    @Test
    public void testInvalidQueryParameter() {
        when(clientContext.queryParams()).thenReturn(createQueryParams("start", "end", "{\"name\":\"categoryA\"}"));
        when(clientContext.pathParams()).thenReturn(createPathParams(BUCKET_NAME));
        NbJSONObject msg = new NbJSONObject();
        msg.put("error", "Parameter Error");
        executeQuery(ApigwResponse.status(400).entity(msg).build(), null);
    }

    /**
     * クエリパラメータのwhereにJSON文字列以外の値を設定し、400エラーとなること
     */
    @Test
    public void testInvalidQueryParameterWhere() {

        when(clientContext.queryParams()).thenReturn(createQueryParams(null, null, "\"name\":\"categoryA\""));
        when(clientContext.pathParams()).thenReturn(createPathParams(BUCKET_NAME));
        NbJSONObject msg = new NbJSONObject();
        msg.put("error", "Parameter Error");
        executeQuery(ApigwResponse.status(400).entity(msg).build(), null);
    }

    /**
     * オブジェクトクエリが失敗すること(404エラー)<br>
     * クエリパラメータ：start, end指定<br>
     * パスパラメータ：not_found_bucket
     */
    @Test
    public void testBucketNotFound() {

        when(clientContext.queryParams()).thenReturn(createQueryParams(null, null, "{\"name\":\"categoryA\"}"));
        when(clientContext.pathParams()).thenReturn(createPathParams("notfound"));
        executeQuery(ApigwResponse.status(404).entity("{\"error\":\"No such bucket\"}").build(), createFailureAnswer(404, "{\"error\":\"No such bucket\"}"), "notfound");
    }

    /**
     * オブジェクトクエリ結果の日付パース処理に失敗し、500エラーとなること<br>
     * パスパラメータ：test_bucket
     */
    @Test
    public void testInvalidQueryResult() {

        when(clientContext.queryParams()).thenReturn(createQueryParams(null, null, null));
        when(clientContext.pathParams()).thenReturn(createPathParams(BUCKET_NAME));

        String err = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.parse("2018-10-31 06:44:35.946");
        } catch (ParseException e) {
            err = e.getMessage();
        }

        executeQuery(ApigwResponse.status(500).entity(err).build(), createSuccessAnswerInvalidUpdatedAt());
    }

    private Answer<Void> createSuccessAnswer() {
        return invocation -> {

            NbCallback<List<NbObject>> cb = invocation.getArgument(1);
            List<NbObject> ret = createSucceedResponseData();

            cb.onSuccess(ret);
            return null;
        };
    }

    private Answer<Void> createSuccessAnswerInvalidUpdatedAt() {

        return invocation -> {

            NbCallback<List<NbObject>> cb = invocation.getArgument(1);
            List<NbObject> ret = createSucceedResponseDataInvalidUpdatedAt();

            cb.onSuccess(ret);
            return null;
        };
    }

    private Answer<Void> createSuccessAnswerTsKeyExist() {

        return invocation -> {

            NbCallback<List<NbObject>> cb = invocation.getArgument(1);
            List<NbObject> ret = createSucceedResponseDataTsKey();

            cb.onSuccess(ret);
            return null;
        };
    }

    private Answer<Void> createSuccessAnswerUnderscoreTsValueOverwrite() {

        return invocation -> {

            NbCallback<List<NbObject>> cb = invocation.getArgument(1);
            List<NbObject> ret = createSucceedResponseDataUnderscoreTsKey();

            cb.onSuccess(ret);
            return null;
        };
    }

    private Answer<Void> createFailureAnswer(int status, String reason) {
        return invocation -> {

            NbCallback<List<NbObject>> cb = invocation.getArgument(1);

            NbErrorInfo err = new NbErrorInfo(reason);
            cb.onFailure(status, err);
            return null;
        };
    }

    private void executeQuery(ApigwResponse verifyResponse, Answer<Void> ans, String bucketName) {

        try {

            NbObjectBucketImpl bucketImpl = spy(new NbObjectBucketImpl(service, bucketName, NbBucketMode.ONLINE));
            bucketImpl.setDescription("Description");

            NbObjectBucketManagerImpl bucketMngMock = mock(NbObjectBucketManagerImpl.class);
            ((NbServiceImpl) service).setObjectBucketManager(bucketMngMock);

            when(context.nebula()).thenReturn(service);
            doReturn(bucketImpl).when(bucketMngMock).getBucket(bucketName);

            if (ans != null) {
                Stubber stub = doAnswer(ans);
                stub.when(bucketImpl).query(any(), any(NbListCallback.class));
            }

            ArgumentCaptor<ApigwResponse> cap = ArgumentCaptor.forClass(ApigwResponse.class);
            doNothing().when(context).succeed(cap.capture());

            ArgumentCaptor<ApigwResponse> capFail = ArgumentCaptor.forClass(ApigwResponse.class);
            doNothing().when(context).fail(capFail.capture());

            InfoMotion.search(null, context);

            if (verifyResponse.getStatus() >= 200 && verifyResponse.getStatus() <= 299) {
                assertEquals(verifyResponse.getEntity(), cap.getValue().getEntity());
                assertEquals(verifyResponse.getStatus(), cap.getValue().getStatus());
            } else {
                assertEquals(verifyResponse.getEntity(), capFail.getValue().getEntity());
                assertEquals(verifyResponse.getStatus(), capFail.getValue().getStatus());
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void executeQuery(ApigwResponse verifyResponse, Answer<Void> ans) {
        executeQuery(verifyResponse, ans, BUCKET_NAME);
    }

    private static class GenericRestExecutorFactory implements NbRestExecutorFactory {
        private NbHttpClient mHttpClient;

        public GenericRestExecutorFactory() {
            mHttpClient = NbHttpClient.getInstance();
        }

        @Override
        public NbRestExecutor create() {
            return new NbGenericRestExecutor(mHttpClient);
        }
    }

    private Map<String, List<String>> createQueryParams(Object start, Object end, String whereJson) {
        Map<String, List<String>> params = new HashMap<>();
        List<String> startList = new ArrayList<>();

        if (start != null) {
            startList.add(start.toString());
            params.put("start", startList);
        }

        if (end != null) {
            List<String> endList = new ArrayList<>();
            endList.add(end.toString());
            params.put("end", endList);
        }

        if (whereJson != null) {
            List<String> whereList = new ArrayList<>();
            whereList.add(whereJson);
            params.put("where", whereList);
        }

        return params;
    }

    private Map<String, String> createPathParams(String bucketName) {
        Map<String, String> pathParam = new HashMap<>();
        pathParam.put("bucketname", bucketName);

        return pathParam;
    }

    private List<NbObject> createApigwResponseData() {
        List<NbObject> ret = new ArrayList<>();

        NbObject obj1 = new NbObject(BUCKET_NAME);
        NbJSONObject objJson1 = new NbJSONObject();
        long timestamp1 = Long.parseLong("1540935875946");
        objJson1.put("_id", "5bd94f53cb6a3b3739e1e92a");
        objJson1.put("createdAt", "2018-10-31T06:44:35.946Z");
        objJson1.put("updatedAt", "2018-10-31T06:44:35.946Z");
        objJson1.put("category", "A");
        objJson1.put("val", "123");
        objJson1.put("ts", timestamp1);
        obj1._setJsonObject(objJson1);

        NbObject obj2 = new NbObject(BUCKET_NAME);
        NbJSONObject objJson2 = new NbJSONObject();
        long timestamp2 = Long.parseLong("1540935876946");
        objJson2.put("_id", "5bd94f53cb6a3b3739e1e92b");
        objJson2.put("createdAt", "2018-10-31T06:44:36.946Z");
        objJson2.put("updatedAt", "2018-10-31T06:44:36.946Z");
        objJson2.put("category", "A");
        objJson2.put("val", "456");
        objJson2.put("ts", timestamp2);
        obj2._setJsonObject(objJson2);

        ret.add(obj1);
        ret.add(obj2);

        return ret;
    }


    private List<NbObject> createApigwResponseDataTsKey() {
        List<NbObject> ret = new ArrayList<>();

        NbObject obj1 = new NbObject(BUCKET_NAME);
        NbJSONObject objJson1 = new NbJSONObject();
        long timestamp1 = Long.parseLong("1540935875946");
        objJson1.put("_id", "5bd94f53cb6a3b3739e1e92a");
        objJson1.put("createdAt", "2018-10-31T06:44:35.946Z");
        objJson1.put("updatedAt", "2018-10-31T06:44:35.946Z");
        objJson1.put("category", "A");
        objJson1.put("val", "123");
        objJson1.put("ts", timestamp1);
        objJson1.put("_ts", "ts_value1");
        obj1._setJsonObject(objJson1);

        NbObject obj2 = new NbObject(BUCKET_NAME);
        NbJSONObject objJson2 = new NbJSONObject();
        long timestamp2 = Long.parseLong("1540935876946");
        objJson2.put("_id", "5bd94f53cb6a3b3739e1e92b");
        objJson2.put("createdAt", "2018-10-31T06:44:36.946Z");
        objJson2.put("updatedAt", "2018-10-31T06:44:36.946Z");
        objJson2.put("category", "A");
        objJson2.put("val", "456");
        objJson2.put("ts", timestamp2);
        objJson2.put("_ts", "ts_value2");
        obj2._setJsonObject(objJson2);

        ret.add(obj1);
        ret.add(obj2);

        return ret;
    }

    private List<NbObject> createSucceedResponseData() {
        List<NbObject> ret = new ArrayList<>();

        NbObject obj1 = new NbObject(BUCKET_NAME);
        obj1.put("_id", "5bd94f53cb6a3b3739e1e92a");
        obj1.put("category", "A");
        obj1.put("val", "123");
        obj1.setUpdatedTime("2018-10-31T06:44:35.946Z");
        obj1.setCreatedTime("2018-10-31T06:44:35.946Z");

        NbObject obj2 = new NbObject(BUCKET_NAME);
        obj2.put("_id", "5bd94f53cb6a3b3739e1e92b");
        obj2.put("category", "A");
        obj2.put("val", "456");
        obj2.setUpdatedTime("2018-10-31T06:44:36.946Z");
        obj2.setCreatedTime("2018-10-31T06:44:36.946Z");

        ret.add(obj1);
        ret.add(obj2);

        return ret;
    }

    private List<NbObject> createSucceedResponseDataInvalidUpdatedAt() {
        List<NbObject> ret = new ArrayList<>();

        NbObject obj1 = new NbObject(BUCKET_NAME);
        obj1.put("_id", "5bd94f53cb6a3b3739e1e92a");
        obj1.put("category", "A");
        obj1.put("val", "123");
        obj1.setUpdatedTime("2018-10-31 06:44:35.946");
        obj1.setCreatedTime("2018-10-31T06:44:35.946Z");

        NbObject obj2 = new NbObject(BUCKET_NAME);
        obj2.put("_id", "5bd94f53cb6a3b3739e1e92b");
        obj2.put("category", "A");
        obj2.put("val", "456");
        obj2.setUpdatedTime("2018-10-31 06:44:36.946");
        obj2.setCreatedTime("2018-10-31T06:44:36.946Z");

        ret.add(obj1);
        ret.add(obj2);

        return ret;
    }

    private List<NbObject> createSucceedResponseDataTsKey() {
        List<NbObject> ret = new ArrayList<>();

        NbObject obj1 = new NbObject(BUCKET_NAME);
        obj1.put("_id", "5bd94f53cb6a3b3739e1e92a");
        obj1.put("category", "A");
        obj1.put("val", "123");
        obj1.put("ts", "ts_value1");
        obj1.setUpdatedTime("2018-10-31T06:44:35.946Z");
        obj1.setCreatedTime("2018-10-31T06:44:35.946Z");

        NbObject obj2 = new NbObject(BUCKET_NAME);
        obj2.put("_id", "5bd94f53cb6a3b3739e1e92b");
        obj2.put("category", "A");
        obj2.put("val", "456");
        obj2.put("ts", "ts_value2");
        obj2.setUpdatedTime("2018-10-31T06:44:36.946Z");
        obj2.setCreatedTime("2018-10-31T06:44:36.946Z");

        ret.add(obj1);
        ret.add(obj2);

        return ret;
    }

    private List<NbObject> createSucceedResponseDataUnderscoreTsKey() {
        List<NbObject> ret = new ArrayList<>();

        NbObject obj1 = new NbObject(BUCKET_NAME);
        obj1.put("_id", "5bd94f53cb6a3b3739e1e92a");
        obj1.put("category", "A");
        obj1.put("val", "123");
        obj1.put("ts", "ts_value1");
        obj1.put("_ts", "_ts_value1");
        obj1.setUpdatedTime("2018-10-31T06:44:35.946Z");
        obj1.setCreatedTime("2018-10-31T06:44:35.946Z");

        NbObject obj2 = new NbObject(BUCKET_NAME);
        obj2.put("_id", "5bd94f53cb6a3b3739e1e92b");
        obj2.put("category", "A");
        obj2.put("val", "456");
        obj2.put("ts", "ts_value2");
        obj2.put("_ts", "_ts_value2");
        obj2.setUpdatedTime("2018-10-31T06:44:36.946Z");
        obj2.setCreatedTime("2018-10-31T06:44:36.946Z");

        ret.add(obj1);
        ret.add(obj2);

        return ret;
    }
}
