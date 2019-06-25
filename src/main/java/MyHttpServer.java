import com.sun.net.httpserver.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


public class MyHttpServer {
    private static JDBCservice jdbc;
    private static HttpServer server;

    public static void main(String[] args) throws IOException, SQLException {
        server = HttpServer.create(new InetSocketAddress(9000), 20);
        server.createContext("/login", new LoginHandler());
        server.createContext("/api/item", new ItemHandler());
        server.createContext("/api/group", new GroupHandler());
        //server.createContext("/api/item/id", new ItemIdHandler());
        server.start();
        jdbc = new JDBCservice();
        ResultSet all = jdbc.selectAllFromItems();
        while (all.next()) {
            System.out.println(all.getInt("id"));
            ItemIdHandler it = new ItemIdHandler(all.getInt("id"));
            server.createContext("/api/item/" + all.getInt("id"), it);
        }
        all = jdbc.selectAllFromGroups();
        while (all.next()) {
            System.out.println(all.getInt("id"));
            GroupIdHandler it = new GroupIdHandler(all.getInt("id"));
            server.createContext("/api/group/" + all.getInt("id"), it);
        }
    }

    public static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<String, String>();
        System.out.println(query);
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }

    static class Auth extends Authenticator {
        @Override
        public Result authenticate(HttpExchange httpExchange) {
            if ("/forbidden".equals(httpExchange.getRequestURI().toString()))
                return new Failure(403);
            else
                return new Success(new HttpPrincipal("c0nst", "realm"));
        }
    }

    static class LoginHandler implements HttpHandler {

        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = MyHttpServer.queryToMap(exchange.getRequestURI().getQuery());
            OutputStream os = exchange.getResponseBody();
            StringBuilder builder = new StringBuilder();
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add("Access-Control-Allow-Origin", "*");
            responseHeaders.set("Content-Type", "text/plain");

            if (params.get("login").equals("admin") && params.get("password").equals("admin")) {
                builder.append("unique_token");
                exchange.sendResponseHeaders(201, builder.length());
            } else {
                exchange.sendResponseHeaders(401, -1);
            }

            byte[] bytes = builder.toString().getBytes();
            os.write(bytes);
            os.close();
        }
    }

    static class ItemHandler implements HttpHandler {

        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder builder = new StringBuilder();
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add("Access-Control-Allow-Origin", "*");

            System.out.println("handle");
            // ---------------------------- GET -----------------------------
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                try {
                    Map<String, String> params = MyHttpServer.queryToMap(exchange.getRequestURI().getQuery());
                    int groupID = Integer.parseInt(params.get("group"));
                    if (groupID <= 0) {
                      ResultSet set = jdbc.selectAllFromItems();
                        JSONArray array = new JSONArray();
                        while (set.next()){
                            array.put(item(set));
                        }
                        exchange.sendResponseHeaders(200, array.toString().getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(array.toString().getBytes());
                        os.close();

                    } else {
                        if (jdbc.itemByGroupID(groupID).next()){
                            ResultSet set = jdbc.itemByGroupID(groupID);
                            JSONArray array = new JSONArray();
                            while (set.next()){
                                array.put(item(set));
                            }
                            exchange.sendResponseHeaders(200, array.toString().getBytes().length);
                            OutputStream os = exchange.getResponseBody();
                            os.write(array.toString().getBytes());
                            os.close();
                        }
                        else {
                            exchange.sendResponseHeaders(404, -1);
                        }
                    }
                }
                catch (SQLException e){
                    e.printStackTrace();
                }
                // ---------------------------- POST -----------------------------
            } else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                JSONObject json = new JSONObject(new JSONTokener(exchange.getRequestBody()));
                System.out.println(json.toMap());

                try {
                    if (jdbc.getItemByID(json.getInt("id")).next()) {
                        if (json.getJSONObject("item").getInt("amount") < 0) {
                            exchange.sendResponseHeaders(409, -1);
                        } else {
                            jdbc.updateItemByID(
                                    json.getInt("id"),
                                    json.getString("name"),
                                    json.getInt("amount"),
                                    json.getString("description"),
                                    json.getString("producer"),
                                    json.getInt("price"),
                                    json.getInt("groupID"));
                            exchange.sendResponseHeaders(204, -1);
                        }
                    } else {
                        exchange.sendResponseHeaders(404, -1);
                    }
                } catch (SQLException e) {

                }


                // ---------------------------- PUT -----------------------------
            } else if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                System.out.println("put");


                try {
                    JSONObject json = new JSONObject(new JSONTokener(exchange.getRequestBody()));
                    if (json.getInt("amount") < 0) {
                        exchange.sendResponseHeaders(409, -1);
                    } else {
                        int id = jdbc.addItem(
                                json.getString("name"),
                                json.getInt("amount"),
                                json.getString("description"),
                                json.getString("producer"),
                                json.getInt("price"),
                                json.getInt("groupID"));

                        builder.append(id);
                        exchange.sendResponseHeaders(200, builder.toString().getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(builder.toString().getBytes());
                        os.close();
                        server.createContext("/api/item/" + id, new ItemIdHandler(id));


                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                //  exchange.sendResponseHeaders(200, builder.toString().getBytes().length);


                // ---------------------------- DELETE -----------------------------
            } else if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                exchange.sendResponseHeaders(404, -1);
            }


        }
        JSONObject item (ResultSet set) throws SQLException{
            JSONObject json = new JSONObject();
            json.put("name", set.getString("name"));
            json.put("amount", set.getString("amount"));
            json.put("description", set.getString("description"));
            json.put("producer", set.getString("producer"));
            json.put("price", set.getString("price"));
            json.put("groupID", set.getString("groupID"));
            return json;
        }
    }

    static class ItemIdHandler implements HttpHandler {
        private int id;

        // /api/good/{id}
        ItemIdHandler(int id) {
            System.out.println("new itemIDhandler " + id);
            this.id = id;
        }

        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder builder = new StringBuilder();
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add("Access-Control-Allow-Origin", "*");
            JSONObject json = new JSONObject();
            String[] uri = exchange.getRequestURI().toString().split("/");
            byte[] bytes = {};
            OutputStream os = exchange.getResponseBody();
            try {
                if (Integer.parseInt(uri[3]) == id && jdbc.getItemByID(id).next()) {

                    if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {

                        json.put("id", id);

                        ResultSet set = jdbc.getItemByID(id);
                        set.next();
                        json.put("name", set.getString("name"));
                        json.put("amount", set.getString("amount"));
                        json.put("description", set.getString("description"));
                        json.put("producer", set.getString("producer"));
                        json.put("price", set.getString("price"));
                        json.put("groupID", set.getString("groupID"));

                        bytes = builder.append(json.toString()).toString().getBytes();
                        exchange.sendResponseHeaders(200, bytes.length);
                        os.write(bytes);
                        os.close();

                    } else if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                        try {
                            if (Integer.parseInt(uri[3]) == id) {
                                jdbc.deleteItemByID(id);
                                exchange.sendResponseHeaders(204, -1);

                            } else {
                                exchange.sendResponseHeaders(404, -1);
                            }
                        } catch (SQLException e) {
                        }


                    }


                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }


        }
    }

    static class GroupHandler implements HttpHandler {

        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder builder = new StringBuilder();
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add("Access-Control-Allow-Origin", "*");
            JSONObject json = new JSONObject(new JSONTokener(exchange.getRequestBody()));


            // ---------------------------- GET -----------------------------
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(404, -1);

                // ---------------------------- POST -----------------------------
            } else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                System.out.println(json.toMap());

                try {
                    if (jdbc.getGroupByID(json.getInt("id")).next()) {
                        jdbc.updateGroupByID
                                (json.getInt("id"), json.getString("name"), json.getString("description"));
                        exchange.sendResponseHeaders(204, -1);

                    } else {
                        exchange.sendResponseHeaders(404, -1);
                    }
                } catch (SQLException e) {

                }


                // ---------------------------- PUT -----------------------------
            } else if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                System.out.println("put");


                try {

                    int id = jdbc.addGroup(
                            json.getString("name"),
                            json.getString("description"));
                    System.out.println("id");
                    builder.append(id);
                    exchange.sendResponseHeaders(200, builder.toString().getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(builder.toString().getBytes());
                    os.close();
                    server.createContext("/api/group/" + id, new ItemIdHandler(id));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                //  exchange.sendResponseHeaders(200, builder.toString().getBytes().length);


                // ---------------------------- DELETE -----------------------------
            } else if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                exchange.sendResponseHeaders(404, -1);
            }


        }
    }

    static class GroupIdHandler implements HttpHandler {
        private int id;

        // /api/good/{id}
        GroupIdHandler(int id) {
            System.out.println("new groupIDhandler " + id);
            this.id = id;
        }

        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder builder = new StringBuilder();
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add("Access-Control-Allow-Origin", "*");
            JSONObject json = new JSONObject();
            String[] uri = exchange.getRequestURI().toString().split("/");
            byte[] bytes = {};
            OutputStream os = exchange.getResponseBody();
            try {
                if (Integer.parseInt(uri[3]) == id && jdbc.getGroupByID(id)!=null) {
                    if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {


                        ResultSet set = jdbc.getGroupByID(id);
                        set.next();
                        json.put("name", set.getString("name"));
                        json.put("description", set.getString("description"));


                        bytes = builder.append(json.toString()).toString().getBytes();
                        exchange.sendResponseHeaders(200, bytes.length);
                        os.write(bytes);
                        os.close();

                    } else if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                        try {
                            if (Integer.parseInt(uri[3]) == id) {
                                jdbc.deleteGroupByID(id);
                                exchange.sendResponseHeaders(204, -1);
                                System.out.println("deleted " + id);
                                server.removeContext("api/group/" + id);
                            } else {
                                exchange.sendResponseHeaders(404, -1);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }


                    }
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }


        }
    }


}

