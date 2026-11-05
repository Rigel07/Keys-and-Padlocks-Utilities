# Utility Machines (Forge 1.20.1)

Máquinas útiles + un sistema completo de candados y llaves.

## Máquinas
- **Reparadora**: 27 huecos; cada objeto dañado se repara solo (20 min de roto a entero).
- **Tubo**: absorbe objetos sueltos en su bloque o sobre una placa de presión pegada, y los lleva por la red al inventario más cercano que los acepte.
- **Autocultivadora**: enterrada bajo la tierra de cultivo; replanta el cultivo ya crecido en cada ciclo.
- **Ascensor**: saltar = subir al siguiente ascensor; agacharse = bajar.
- **Taladrador**: se instala en una vagoneta, pica, pone carriles y guarda lo picado.

## Candados y llaves
- **Mesa de cerradura**: crea juegos llave + candado con código único (madera, cobre, hierro, diamante, netherita) y copia llaves (original → copia → copia desgastada).
- **Candado**: en la mano + clic derecho sobre puertas, trampillas, portones, cofres, barriles, hornos...
- **Llave**: en cualquier mano, usas el bloque con normalidad. Agachado + clic: retira el candado.
- **Ganzúa**: fuerza un candado (probabilidad según material; netherita imposible). Queda abierto 10 s. Si falla, suena la alarma y se avisa al dueño.
- **Llavero**: guarda hasta 12 llaves.
- **Llave maestra**: abre todo salvo netherita.
- Diamante y netherita resisten explosiones y mobs destructivos; sin llave, picar un bloque cerrado cuesta mucho más.

## Compilar
Sube el .zip a GitHub con el `build.yml` en `.github/workflows/`, o en local: `gradle build` (JDK 17, Gradle 8.1.1).
Textures: algunas son placeholders generados por código; puedes sustituirlas en `assets/utilitymachines/textures`.
