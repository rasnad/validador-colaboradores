API Mock de Validación de Colaboradores

Esta aplicación es un servidor Java simple (sin frameworks externos) diseñado para simular una base de datos de colaboradores. Su función principal es validar si una persona existe en el registro de la empresa mediante su tipo y número de documento.

🚀 Cómo ejecutar la aplicación

Requisitos: Java JDK 8 o superior instalado.

Compilar el código:

javac ColaboradorMockServer.java


Iniciar el servidor:

java ColaboradorMockServer


El servidor iniciará en el puerto 8082.

📡 Endpoints Disponibles

Validar Documento

Devuelve la información del colaborador si los datos coinciden exactamente.

URL: http://localhost:8082/openapi/users/document

Método: GET

Parámetros (Query Params):
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| number | String | Número del documento (ej: 12345678) |
| type | String | Tipo de documento (ej: Cedula de identidad) |

🧪 Ejemplos de Prueba (cURL)

Puedes importar estos comandos directamente en Postman o ejecutarlos en tu terminal.

✅ Casos Exitosos (Datos Hardcodeados)

1. Buscar por Cédula de Identidad (Juan Perez)

curl --location 'http://localhost:8082/openapi/users/document?number=12345678&type=Cedula%20de%20identidad'


2. Buscar por Pasaporte (Maria Gomez)

curl --location 'http://localhost:8082/openapi/users/document?number=AABB1122&type=Pasaporte'


3. Buscar por Legajo (Carlos Rodriguez)

curl --location 'http://localhost:8082/openapi/users/document?number=L-998877&type=Legajo'


❌ Casos de Error

1. Usuario No Encontrado (404)
Simula cuando el documento no existe en la base de datos.

curl --location 'http://localhost:8082/openapi/users/document?number=99999999&type=Cedula%20de%20identidad'


Respuesta esperada:

{
    "error": "Usuario no encontrado"
}


2. Faltan Parámetros (400)
Error cuando se olvida enviar el tipo o el número.

curl --location 'http://localhost:8082/openapi/users/document?number=12345678'


Respuesta esperada:

{
    "error": "Faltan parametros obligatorios: 'number' y 'type'"
}


3. Método Incorrecto (405)
Intentar hacer un POST en lugar de un GET.

curl --location --request POST 'http://localhost:8082/openapi/users/document?number=12345678&type=Cedula%20de%20identidad'


Respuesta esperada:

{
    "error": "Metodo no permitido"
}
