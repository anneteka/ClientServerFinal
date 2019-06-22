import com.sun.net.httpserver.*;
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
        //server.createContext("/api/item/id", new ItemIdHandler());
        server.start();
        jdbc = new JDBCservice();
        ResultSet all = jdbc.Read();
        while (all.next()) {
            server.createContext("/api/item/" + all.getInt("id"), new ItemIdHandler(all.getInt("id")));
        }
    }

    public static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<String, String>();
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
            JSONObject json = new JSONObject(new JSONTokener(exchange.getRequestBody()));

            if (exchange.getRequestHeaders().get("auth") != null && exchange.getRequestHeaders().get("auth").get(0).equals("unique_token")) {


                // ---------------------------- GET -----------------------------
                if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    exchange.sendResponseHeaders(404, -1);

                    // ---------------------------- POST -----------------------------
                } else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    System.out.println(json.toMap());
                    System.out.println(json.getJSONObject("item").getString("name"));

                    try{
                        if (jdbc.GetById(json.getInt("id"))!=null){
                            if (json.getJSONObject("item").getInt("amount")<0){
                                exchange.sendResponseHeaders(409, -1);
                            }
                            else {
                                jdbc.UpdateByID(json.getInt("id"), json.getJSONObject("item").getString("name"), json.getJSONObject("item").getInt("amount"));
                                exchange.sendResponseHeaders(204, -1);
                            }
                        }
                        else{
                            exchange.sendResponseHeaders(404, -1);
                        }
                    }
                    catch (SQLException e){

                    }


                    // ---------------------------- PUT -----------------------------
                } else if (exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                    System.out.println("put");


                    try {
                        if (json.getInt("amount")<0){
                            exchange.sendResponseHeaders(409, -1);
                        }
                        else {
                            int id = jdbc.Create(json.getString("name"), json.getInt("amount"));
                            System.out.println("id");
                            builder.append(id);
                            exchange.sendResponseHeaders(200, builder.toString().getBytes().length);
                            OutputStream os = exchange.getResponseBody();
                            os.write(builder.toString().getBytes());
                            os.close();
                            server.createContext("api/item" + id, new ItemIdHandler(id));

                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                  //  exchange.sendResponseHeaders(200, builder.toString().getBytes().length);


                    // ---------------------------- DELETE -----------------------------
                } else if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                    exchange.sendResponseHeaders(404, -1);
                }

            } else {
                System.out.println("not auth");
                exchange.sendResponseHeaders(403, -1);
            }

        }
    }

    static class ItemIdHandler implements HttpHandler {
        private int id;

        // /api/good/{id}
        ItemIdHandler(int id) {
            this.id = id;
        }

        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder builder = new StringBuilder();
            Headers responseHeaders = exchange.getResponseHeaders();
            JSONObject json = new JSONObject();
            String[] uri = exchange.getRequestURI().toString().split("/");
            byte[] bytes = {};
            OutputStream os = exchange.getResponseBody();
            if (exchange.getRequestHeaders().get("auth") != null && exchange.getRequestHeaders().get("auth").get(0).equals("unique_token")) {
                if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {

                    if (Integer.parseInt(uri[3]) == id) {
                       json.put("id", id);
                       try {
                           ResultSet set = jdbc.GetById(id);
                           set.next();
                           json.put("name", set.getString("name"));
                           json.put("amount", set.getString("amount"));
                       }
                       catch (SQLException e){

                       }
                        bytes = builder.append(json.toString()).toString().getBytes();
                        exchange.sendResponseHeaders(200, bytes.length);
                        os.write(bytes);
                        os.close();
                    } else {
                        exchange.sendResponseHeaders(404, -1);
                    }
                } else if (exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                    try {
                        if (Integer.parseInt(uri[3]) == id) {
                            jdbc.DeleteByID(id);
                            exchange.sendResponseHeaders(204,-1);
                            server.removeContext("api/item/"+id);
                        }
                        else {
                            exchange.sendResponseHeaders(404, -1);
                        }
                    }
                    catch (SQLException e){}


                }
            } else {
                System.out.println("not auth");
                exchange.sendResponseHeaders(403, -1);
            }

        }
    }

}

/**
+ get login
+ delete /api/good/{id}
+ get /api/good/{id}
+ put /api/good
- post /api/good

*/