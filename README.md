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