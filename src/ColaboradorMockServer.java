import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * API Mock para validación de Colaboradores.
 * * Para ejecutar este archivo:
 * 1. Compilar: javac ColaboradorMockServer.java
 * 2. Ejecutar: java ColaboradorMockServer
 * * Nuevo Endpoint:
 * GET /openapi/users/document?number={numero}&type={tipo}
 * * Ejemplo de uso:
 * http://localhost:8080/openapi/users/document?number=12345678&type=Cedula%20de%20identidad
 */
public class ColaboradorMockServer {

    private static final int PORT = 8082;
    private static final List<UserRecord> BASE_DE_DATOS_MOCK = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        inicializarDatos();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Nuevo path del endpoint
        server.createContext("/openapi/users/document", new ValidacionHandler());

        server.setExecutor(null);
        System.out.println("-------------------------------------------------");
        System.out.println("API Mock iniciada en el puerto " + PORT);
        System.out.println("Endpoint disponible: /openapi/users/document");
        System.out.println("Ejemplo: http://localhost:8082/openapi/users/document?number=12345678&type=Cedula%20de%20identidad");
        System.out.println("-------------------------------------------------");
        server.start();
    }

    private static void inicializarDatos() {
        // Datos hardcodeados con la nueva estructura y tipos
        BASE_DE_DATOS_MOCK.add(crearUsuario("Juan", "Perez", "Cedula de identidad", "12345678"));
        BASE_DE_DATOS_MOCK.add(crearUsuario("Maria", "Gomez", "Pasaporte", "AABB1122"));
        BASE_DE_DATOS_MOCK.add(crearUsuario("Carlos", "Rodriguez", "Legajo", "L-998877"));

        System.out.println("Base de datos cargada con " + BASE_DE_DATOS_MOCK.size() + " registros.");
    }

    private static UserRecord crearUsuario(String nombre, String apellido, String tipoDoc, String numDoc) {
        PersonalData pd = new PersonalData(nombre, apellido, tipoDoc, numDoc);
        // Generamos un ID estilo Mongo aleatorio para el mock
        String fakeMongoId = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        return new UserRecord(fakeMongoId, pd);
    }

    static class ValidacionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleGetRequest(exchange);
            } else {
                sendResponse(exchange, 405, "{\"error\": \"Metodo no permitido\"}");
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = queryToMap(query);

            String numeroBuscado = params.get("number");
            String tipoBuscado = params.get("type");

            if (numeroBuscado == null || tipoBuscado == null) {
                sendResponse(exchange, 400, "{\"error\": \"Faltan parametros obligatorios: 'number' y 'type'\"}");
                return;
            }

            System.out.println("Buscando -> Tipo: " + tipoBuscado + " | Numero: " + numeroBuscado);

            // Búsqueda exacta por tipo y número dentro de personalData
            Optional<UserRecord> resultado = BASE_DE_DATOS_MOCK.stream()
                    .filter(u -> {
                        PersonalData pd = u.getPersonalData();
                        // Normalizamos strings para evitar errores por mayúsculas/minúsculas simples
                        return pd.getDocumentNumber().equalsIgnoreCase(numeroBuscado) &&
                                pd.getNumberOfDocument().equalsIgnoreCase(tipoBuscado); // numberOfDocument es el TIPO segun tu JSON
                    })
                    .findFirst();

            if (resultado.isPresent()) {
                UserRecord u = resultado.get();
                PersonalData pd = u.getPersonalData();

                // Construcción del JSON anidado
                String jsonResponse = String.format(
                        "{" +
                                "\"_id\": \"%s\"," +
                                "\"personalData\": {" +
                                "\"firstName\": \"%s\"," +
                                "\"lastName\": \"%s\"," +
                                "\"numberOfDocument\": \"%s\"," +
                                "\"documentNumber\": \"%s\"" +
                                "}" +
                                "}",
                        u.getId(),
                        pd.getFirstName(),
                        pd.getLastName(),
                        pd.getNumberOfDocument(),
                        pd.getDocumentNumber()
                );
                sendResponse(exchange, 200, jsonResponse);
            } else {
                // Si no se encuentra, devolvemos 404
                sendResponse(exchange, 404, "{\"error\": \"Usuario no encontrado\"}");
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }

        private Map<String, String> queryToMap(String query) {
            Map<String, String> result = new HashMap<>();
            if (query == null) return result;
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    // Decodificar valores de URL (espacios, caracteres especiales)
                    String key = entry[0];
                    String value = java.net.URLDecoder.decode(entry[1], StandardCharsets.UTF_8);
                    result.put(key, value);
                }
            }
            return result;
        }
    }

    // --- Nuevas Clases de Modelo ---

    // Clase contenedora principal
    static class UserRecord {
        private String _id;
        private PersonalData personalData;

        public UserRecord(String _id, PersonalData personalData) {
            this._id = _id;
            this.personalData = personalData;
        }

        public String getId() { return _id; }
        public PersonalData getPersonalData() { return personalData; }
    }

    // Clase anidada con los datos personales
    static class PersonalData {
        private String firstName;
        private String lastName;
        private String numberOfDocument; // OJO: En tu JSON esto contiene el TIPO ("tipoDeDocuemnto")
        private String documentNumber;   // Este contiene el NUMERO ("12345678")

        public PersonalData(String firstName, String lastName, String numberOfDocument, String documentNumber) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.numberOfDocument = numberOfDocument;
            this.documentNumber = documentNumber;
        }

        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getNumberOfDocument() { return numberOfDocument; }
        public String getDocumentNumber() { return documentNumber; }
    }
}