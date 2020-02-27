# spring-cloud-security-with-zuul

SPRING CLOUD STACK
- eureka-server
 server eureka per registrare tutti gli altri server: consente di chiamare i server tra di loro anche con il nome settato nelle properties spring.application.name

- login-service 
server che si occupa di autenticare, prelevare i ruoli e creare un token jwt con username e ruoli

- auth-service
server zuul che consente di effettuare il routing verso gli end point configurati (valida il token e ruoli) e possono essere gestiti i ruoli sugli endpoint anche
di url non java

- micro-service
microservizio a se che puo essere invocato standoalone o coperto da security come definito nell'esempio


####
DOCKER
- dare permessi di esecuzione a tutti i file .sh:
chmod 755 *.sh

- modificare il file di configurarione:
./auth-service/src/main/resources/application.properties 

nella sezione custom è possibile definire una nuova rotta:
custom-route.routes.1.path=/api/test**
e se questa è accessibile a tutti settare la variabile:
custom-route.routes.1.permitAll=true
altrimenti definire il ruolo:
custom-route.routes.1.role=ADMIN


# custom route example use:
#custom-route.routes.1.path=/api/test**
#custom-route.routes.1.role=
#custom-route.routes.1.permitAll=true
custom-route.routes.1.path=/login-service/api/login
custom-route.routes.1.permitAll=true
custom-route.routes.2.path=/login-service/api/login-keycloak
custom-route.routes.2.permitAll=true
custom-route.routes.3.path=/micro-service/api/sayHello/admin
custom-route.routes.3.role=ADMIN
custom-route.routes.4.path=/micro-service/api/sayHello/user
custom-route.routes.4.role=USER

per tutte le rotte non definite automaticamente per accedervi sarà necessario essere autenticati

se la rotta è esterna (altro indirizzo al di fuori)
utilizzare le impostazioni dirette di zuul:
creare una rotta zuul dandogli un nome qualsiasi (esempio test), definire la rotta verso cui deviare (esempio /test/**) e poi definire in un altra riga l'url vero di destinazione (esempio https://jsonplaceholder.typicode.com/posts)
Manual mapping of routes using static URLs
zuul.routes.test.path = /test/**
zuul.routes.test.url = https://jsonplaceholder.typicode.com/posts


- lanciare il comando ./start.sh

comandi curl:

### login
curl -X POST http://localhost:9092/login-service/api/login -H 'Content-Type: application/json' -d '{"userCredential":"admin","password": "admin","subDepartmentId": 0}'


### TOKEN login
TOKEN=$(curl -X POST http://localhost:9092/login-service/api/login -H 'Content-Type: application/json' -d '{"userCredential":"admin","password": "admin","subDepartmentId": 0}')


## SERVICE admin
curl -X GET 'http://localhost:9092/micro-service/api/sayHello/admin' -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json'

## SERVICE user
curl -X GET 'http://localhost:9092/micro-service/api/sayHello/user' -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json'

### service remapping url internet
curl -X GET 'http://localhost:9092/test/' -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json'