dom
Cuando se tiene una página html, internamente se tiene por detras un codigo que se puede traducir en un arbol en donde tenemos la cabeza y los nodos hijos, dependiendo la complejidad se tiene esta estructura. Entonces esta traducción es esto. Estos se pueden alterar perfectamente y se puede ver representada en el html

En las pruebas unitarias se trabaja con lógica aislada (nosotros no dependemos de ninguna libreria externa, al igual que el back). En la integracion se simula la navegación, con el dom, en la manipulación de esta misma. Las pruebas de integración, debido al dom, en tema de velocidad es un poco más lenta a comparación de las unitarias. Un ejemplo muy acertado para saber exactamente como es una prueba de integracion es simular un clic en un boton y ver si aparece un error en pantalla. 

Ciclo de vida (muy parecido al junit)
- beforeAll (Una sola vez antes de que comiencen las pruebas del archivo, inicializar servicios pesados -ideal)
-beforeEach
-afterEach
-afterAll
**COMPLETAR**

Buenas practicas en el testing frontend: 

-Aislamiento del DOM: debemos usar el beforeEach o afterEach para evitar que el estado de un test afecte al siguiente
-Aserciones Atómicas: Cada test debe evaluar un unico comportamiento de negocio, no flujos enteros en una sola prueba gigantescas.
-No depender de APIs Reales: 
-Usar nombres descriptivos: Debe leerse como una especificacion de negocio