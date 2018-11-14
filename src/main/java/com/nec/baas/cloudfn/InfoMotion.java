package com.nec.baas.cloudfn;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import com.nec.baas.cloudfn.sdk.ApigwResponse;
import com.nec.baas.cloudfn.sdk.Context;
import com.nec.baas.core.NbErrorInfo;
import com.nec.baas.core.NbListCallback;
import com.nec.baas.core.NbService;
import com.nec.baas.json.NbJSONArray;
import com.nec.baas.json.NbJSONObject;
import com.nec.baas.json.NbJSONParser;
import com.nec.baas.object.NbClause;
import com.nec.baas.object.NbObject;
import com.nec.baas.object.NbObjectBucket;
import com.nec.baas.object.NbQuery;

public class InfoMotion {
    /** QueryParameter : start */
    private static final String QUERY_PARAM_START = "start";
    /** QueryParameter : end */
    private static final String QUERY_PARAM_END = "end";
    /** QueryParameter : where */
    private static final String QUERY_PARAM_WHERE = "where";
    /** PathParameter : bucketname */
    private static final String PATH_PARAM_BUCKETNAME = "bucketname";

    public static void search(Map<String, Object> event, Context context) {
        NbClause clause = new NbClause();
        NbQuery query = new NbQuery();
        String bucketName = context.clientContext().pathParams().get(PATH_PARAM_BUCKETNAME);
        NbService nebula = context.nebula();

        context.logger().debug("headers = " + context.clientContext().headers());
        context.logger().debug("queryParams = " + context.clientContext().queryParams());
        context.logger().debug("pathParams = " + context.clientContext().pathParams());

        Map<String, List<String>> queryParams = context.clientContext().queryParams();

        // QueryParams
        query.setClause(clause);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        try {
            if (queryParams.containsKey(QUERY_PARAM_START)) {
                long start = Long.parseLong(queryParams.get(QUERY_PARAM_START).get(0));

                NbClause startClause = new NbClause();
                startClause.greaterThanOrEqual("updatedAt", sdf.format(new Date(start)));
                clause.and(startClause);
            }

            if (queryParams.containsKey(QUERY_PARAM_END)) {
                long end = Long.parseLong(queryParams.get(QUERY_PARAM_END).get(0));

                NbClause endClause = new NbClause();
                endClause.lessThanOrEqual("updatedAt", sdf.format(new Date(end)));
                clause.and(endClause);
            }
        } catch (NumberFormatException e) {
            ApigwResponse response = ApigwResponse.status(400).entity("Parameter Error").build();
            context.fail(response);
            context.logger().warn("query parameter error", e);
            return;
        }

        if (queryParams.containsKey(QUERY_PARAM_WHERE)) {
            String whereJson = queryParams.get(QUERY_PARAM_WHERE).get(0);
            NbJSONObject whereJsonObject = NbJSONParser.parse(whereJson);
            if (whereJsonObject == null) {
                ApigwResponse response = ApigwResponse.status(400).entity("Parameter Error").build();
                context.fail(response);
                context.logger().warn("query parameter error");
                return;

            }
            NbClause where = NbClause.fromJSONObject(whereJsonObject);
            clause.and(where);
        }

        // Projection
        NbJSONObject projection = new NbJSONObject();
        projection.put("etag", 0);
        projection.put("ACL", 0);
        query.setProjection(projection);

        // クエリ実行
        NbObjectBucket bucket = nebula.objectBucketManager().getBucket(bucketName);
        bucket.query(query, new NbListCallback<NbObject>() {
            @Override
            public void onSuccess(List<NbObject> objects) {

                try {
                    context.succeed(createResponse(objects));
                } catch (ParseException e) {
                    ApigwResponse response = ApigwResponse.status(500).entity(e.getMessage()).build();
                    context.fail(response);
                }
            }

            @Override
            public void onFailure(int statusCode, NbErrorInfo errorInfo) {
                ApigwResponse response = ApigwResponse.status(statusCode).entity(errorInfo.getReason()).build();
                context.fail(response);
            }
        });
    }

    /**
     * Data Source スキーマに沿ったレスポンスを作成する<br>
     * https://docs.enebular.com/ja/infomotion/DataSourceSchema.html
     * 
     * @param objects クエリ実行結果
     * @return レスポンス
     * @throws ParseException ParseException
     */
    private static ApigwResponse createResponse(List<NbObject> objects) throws ParseException {

        NbJSONArray<NbJSONObject> response = new NbJSONArray<>();
        for (NbObject object : objects) {
            NbJSONObject json = new NbJSONObject();
            NbJSONObject value = new NbJSONObject();

            // timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(object.getUpdatedTime()));

            long timestamp = cal.getTimeInMillis();
            json.put("timestamp", timestamp);

            // value
            value.put("_id", object.getObjectId());
            value.put("createdAt", object.getCreatedTime());
            value.put("updatedAt", object.getUpdatedTime());

            for (String key : object.keySet()) {
                value.put(key, object.get(key));
            }
            json.put("value", value);

            response.add(json);
        }

        return ApigwResponse.ok().entity(response.toString()).build();
    }
}
