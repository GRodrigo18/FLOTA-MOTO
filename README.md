# MotoFlota Pro

App Android (Kotlin + Jetpack Compose + Room) para administrar una flota de mototaxis alquiladas a choferes:
cuota diaria, garantía y deudas, SOAT / revisión técnica, taller e inventario, caja y respaldos.

## Cómo ver la app (para mostrarla al cliente)

**Opción A – Emulador (para ti)**
1. Instala [Android Studio](https://developer.android.com/studio) y abre esta carpeta (`File > Open`).
2. Espera a que termine "Gradle Sync" (la primera vez descarga todo; requiere internet).
3. `Tools > Device Manager > Create Device` (por ejemplo Pixel 7) y ejecuta con el botón ▶ **Run**.

**Opción B – APK para el celular del cliente**
1. En Android Studio: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
2. Toma el archivo en `app/build/outputs/apk/debug/app-debug.apk`.
3. Envíalo por correo o Google Drive. En el celular del cliente: abrir el archivo y permitir
   "Instalar apps desconocidas" cuando Android lo pida.
4. Para una versión de entrega firmada con tu propia llave, define las variables de entorno
   `KEYSTORE_PATH`, `STORE_PASSWORD` y `KEY_PASSWORD` y ejecuta `Build > Generate Signed Bundle / APK`.

## Usuarios (varias personas en el mismo teléfono)

- **Primer uso:** se crea el dueño como **Administrador** (nombre + PIN). Si ya usabas la versión
  anterior, tu PIN de siempre se convierte solo en tu usuario Administrador; no hay que hacer nada.
- **Crear otro usuario:** toca el círculo con tu inicial (arriba a la derecha) › **Usuarios** ›
  **Agregar usuario**. Escribe nombre, elige el rol y un PIN de 4 a 6 números, y dáselo a la persona.
- **Roles:**
  - *Administrador:* ve todo, incluida la Caja, los respaldos y los usuarios.
  - *Cobrador:* registra pagos, deudas, flota y taller; **no** ve la pestaña Caja, ni el dinero
    disponible, ni respaldos ni usuarios.
- **Al ingresar** con varios usuarios, cada persona toca su nombre y escribe su propio PIN.
- **¿Olvidó su PIN?** Un Administrador le pone uno nuevo desde Usuarios (ícono de llave). Cada uno
  puede cambiar el suyo en el menú › **Cambiar mi PIN**.
- Con un solo usuario se puede entrar con la huella del teléfono. Con varios usuarios la huella se
  desactiva a propósito: quien conozca el bloqueo del celular podría entrar como otra persona.
  Recomendación: ten **dos administradores**, así si uno olvida su PIN el otro se lo restablece.
- Siempre debe quedar al menos un Administrador (la app no deja borrarlo ni quitarle el rol).

## Primer ingreso (versión anterior)
La app pide crear un **PIN de 4 a 6 dígitos** (y opcionalmente nombre y correo para respaldos).
Después se entra con ese PIN o con la huella / bloqueo del teléfono. Si se olvida el PIN, se verifica
con la huella o el bloqueo de pantalla del teléfono y se crea uno nuevo. Tras 5 intentos fallidos
el ingreso se bloquea 30 segundos.

## Cambios de esta versión
- Login falso reemplazado por acceso real con PIN (hash con sal; el PIN nunca se guarda) + huella.
- Eliminados el correo personal del desarrollador, la contraseña precargada y las cuentas Google de ejemplo.
- Eliminado el estado "logueado" por defecto: antes se veía la app unos instantes sin pedir acceso.
- Eliminados los textos falsos ("Base cifrada", "Sincronizado hoy", "Sincronizar en la nube"):
  los respaldos son manuales (correo / compartir archivo) y la app lo dice claramente.
- Eliminados datos de demostración: ya no se crean 50 botellas de aceite ni 10 llantas inexistentes;
  el stock empieza en cero y se registra con compras reales. Teléfono por defecto ya no es un número inventado.
- Base de datos: ya no se borra todo al cambiar de versión (solo las versiones 1-5 de pruebas pueden reiniciarse);
  se exporta el esquema a `app/schemas` para escribir migraciones.
- `allowBackup=false` (los datos financieros no se suben a la copia automática de Android).
- Gradle: quitadas dependencias sin uso (Firebase, Retrofit, OkHttp, Moshi, Navigation, etc.) y plugins
  sin uso; corregida la firma de debug (apuntaba a un `debug.keystore` que no está en el repositorio y
  hacía fallar la compilación en cualquier otra computadora).
- Pruebas de plantilla reemplazadas por pruebas reales (ver "Pruebas automáticas").
- **Error corregido:** los días de SOAT, el estado "Por Vencer" y el avance del contrato se guardaban
  una sola vez al crear la moto, así que a los meses seguían diciendo "365 días" y las alertas nunca
  saltaban. Ahora se recalculan al leer los datos (`FleetDates` + `FleetRepository.refreshDerivedFields`).
  Se actualizan al abrir la app o al cambiar cualquier dato.
- Archivos de más de 1.000 líneas divididos por tema (`FleetDialogs`, `DailyPaymentsScreen`,
  `DashboardScreen` → 11 archivos de 250 a 780 líneas). Se comprobó automáticamente que las 26 funciones
  quedaron idénticas, sin duplicados.

## Alertas, notificaciones y respaldo automático (esta versión)

- **Alertas corregidas.** Antes el contrato solo avisaba si era semanal y el stock de aceite
  exigía tener inventario cargado para aparecer en el dashboard. Ahora `AlertEngine.kt` centraliza
  las reglas (SOAT a 15 días, contrato a 3 días, cobros del día sin pagar desde las 5pm, stock bajo)
  y las usa tanto la pantalla como el aviso en segundo plano, así nunca se contradicen. 8 pruebas en
  `AlertEngineTest.kt`.
- **Aviso diario aunque la app esté cerrada.** A las 8:00 avisa de SOAT, contratos, deudas y stock;
  a las 7:30pm avisa de las cuotas del día sin cobrar y guarda el respaldo automático. Se reprograma
  él mismo cada vez que corre, a la hora exacta del día siguiente (evita el atraso típico de las
  tareas periódicas de Android, que se van corriendo de horario con los días).
- **Interruptor de notificaciones que no servía.** Existía en la pantalla, se guardaba, pero nunca
  se revisaba antes de enviar un aviso. Ya está conectado: si lo apagas, el aviso diario no suena
  (el respaldo automático se sigue guardando igual).
- **Carpeta "Sincronizar".** En Respaldos puedes elegir una carpeta del teléfono (por ejemplo, una
  carpeta llamada "Sincronizar" dentro de tu app de Google Drive instalada). Cada tarde se guarda
  ahí un respaldo del día, reemplazando el de ayer y conservando los últimos 14. Si esa carpeta está
  dentro de una app de nube, la nube la sube sola; la app no necesita ninguna clave para esto.
  Puedes forzar un respaldo o restaurar desde esa carpeta con un botón.
- **Respaldo por correo, antes roto.** El botón "Enviar a mi correo" pegaba todo el JSON como texto
  dentro del cuerpo del correo: con una flota real esto se corta (casi todas las apps de correo
  limitan el tamaño de un enlace "mailto") y el respaldo llegaba incompleto. Ahora se adjunta como
  archivo `.json` real.
- Se pide el permiso de notificaciones (Android 13+) al ingresar, y se programan los dos avisos
  diarios automáticamente.

## Cambios de la versión 1.6 (días sin pagar por moto, 14 nov 2026)

- **Aviso de días sin pagar, por moto y con las fechas**: cada moto que debe cuota y no pagó tiene su propio recordatorio, por ejemplo "No pagó 2 días • Moto 3 — Sin pagar: jue 12 nov, vie 13 nov • S/ 70.00". El día de hoy cuenta desde las 17:00. Reemplaza al antiguo "Cobros pendientes hoy".
- **Se resuelve día por día**: al tocar el aviso (o "Sin pagar: N días" en el cobro diario) se abre la lista de días con dos botones: **Pagó** (registra la cuota con esa fecha y entra a Caja) o **No trabajó** (no se cobra). Cuando ya no queda ningún día, el aviso se quita solo.
- **Taller y moto inactiva no cuentan**: los días que la moto estuvo en el taller o inactiva se marcan solos como "No trabajó" cuando vuelve a trabajar.
- **No se inventan faltas viejas**: los días sin pagar se cuentan desde el primer cobro registrado en la app (o desde que se instaló esta versión), desde el inicio del contrato y desde que se registró la moto; como máximo los últimos 60 días.
- **Moto inactiva = sin deudas**: al dejar una moto Inactiva (o finalizar su contrato) se anula **toda** su deuda, garantía incluida. No entra ni sale de Caja y queda en el historial de Deudas como "Anulado"; si fue un error, se toca y se deshace.
- **Cobro diario**: muestra "NO TRABAJÓ" en los días marcados así, y "Marcar todas" no les cobra.
- **Guía**: un paso nuevo de NOVEDADES explica el aviso de días sin pagar.
- Versión 7 / 1.6. Sin cambios en la base de datos (sigue en la versión 7): actualizar no toca los datos.

## Cambios de la versión 1.5 (recordatorios que no se pierden, 14 nov 2026)

- **Los recordatorios ya no se borran a mano**: un aviso de contrato por vencer, SOAT, revisión, deuda, cobros del día o stock se queda hasta que **se resuelve** (renovar o editar el contrato en la moto, renovar el SOAT, pagar la deuda, marcar la revisión, comprar stock, cobrar la cuota). Al resolverlo se quita solo.
- **Inicio vs campana**:
  - En **Inicio**, el ojo tachado ("Quitar de Inicio") lo saca de la pantalla para que no se vea cargado. **Sigue en la campana** 🔔. Si hay ocultos, Inicio muestra "N recordatorio(s) en la campana" y al tocarlo se abre la campana.
  - Si el problema empeora (ej. el contrato pasa de "por vencer" a "vencido", o el SOAT vence), vuelve a salir en Inicio.
  - La **campana** ("Recordatorios pendientes") muestra **todos**, siempre, del más urgente al menos urgente, sin botón para borrar. Los ocultos en Inicio dicen "Oculto en Inicio" y tienen "Mostrar en Inicio". El número rojo de la campana cuenta todos.
- **Tocar un recordatorio lleva a resolverlo**: contrato → ficha de la moto en Flota (Renovar / Editar); SOAT y revisión → editar la moto; deuda → registrar abono; cobros del día → Pagos; stock → Taller.
- **Notificación fija y silenciosa** en el celular: "MotoFlota: N recordatorios pendientes", sin sonido ni vibración, que se queda mientras haya pendientes y desaparece sola cuando ya no hay. En Android 14 o más nuevo el sistema deja deslizarla, pero vuelve a aparecer al abrir la app y en los avisos de las 8:00 y 19:30. Al tocarla se abre la app (pide el PIN si hace falta) con la campana abierta. Cada recordatorio sigue avisando una vez al día mientras no se resuelva, aunque esté oculto en Inicio. Respeta "Silenciar avisos".
- **Otros arreglos de esta versión**:
  - Una moto **inactiva** que quedó con deuda sigue recordando la deuda hasta que se pague (antes el aviso desaparecía al finalizar el contrato).
  - El SOAT que vence **hoy** dice "Vence HOY", no "VENCIDO" (era un día antes de tiempo).
  - Con la app abierta, los recordatorios se actualizan solos con la hora (cobros de las 17:00, vencimientos a medianoche).
  - Si se estaba llenando una ventana (ej. editando una moto), la campana espera a que se cierre y no borra lo escrito.
  - "Cobros pendientes hoy" quitado de Inicio vuelve a salir al día siguiente si hay cuotas sin cobrar.
- **Guía actualizada**: quien ya hizo la guía verá una sola vez solo las **NOVEDADES** de esta versión (la campana de recordatorios y el ojo de Inicio).
- Compilada con el SDK de Android: Lint sin errores y 83 pruebas automáticas pasan.
- Versión 6 / 1.5. Sin cambios en la base de datos (sigue en la versión 7): actualizar no toca los datos.

## Cambios de la versión 1.4 (auditoría completa, 6 nov 2026)

### Errores corregidos
- **Deudas que se borraban solas**: al pasar una moto al taller, sacarla o editarla con la ficha abierta desde antes, se guardaba la moto con la deuda vieja y se perdían cargos o abonos recientes. Ahora las deudas solo cambian con cargos, abonos o anulaciones, y los cambios de estado tocan solo el estado.
- **Ficha de la moto desactualizada**: mostraba la deuda o el estado de antes de un pago. Ahora siempre muestra los datos actuales.
- **Fechas escritas a mano** (31/12/2026) dejaban sin funcionar las alertas de SOAT y contrato. Ahora se convierten a 2026-12-31 y se rechazan fechas imposibles o un fin de contrato anterior al inicio.
- **"Cancelar contrato" con un solo toque** quitaba al chofer sin preguntar. Ahora es "Finalizar" y pide confirmación.
- **Revisión del mes** en la ficha decía siempre "Al día" aunque estuviera pendiente.
- **Contrato vencido hace semanas** avisaba "vence hoy". Ahora dice "Contrato vencido (hace N días)".
- **Cobro directo**: un monto mal escrito se guardaba como S/ 35 sin avisar; ya no aparecen motos inactivas.
- **Taller**: al borrar el número de botellas o llantas para escribir otro, volvía solo a "2" o "1" (y quedaba "23"). Ya se puede borrar y escribir normal.
- **Compras del almacén** venían con 100 botellas / 10 llantas ya escritas (fácil de comprar de más sin querer). Ahora la cantidad empieza vacía.
- **Garantía vacía** al registrar una moto se tomaba como S/ 200 / S/ 100. Ahora vacío = 0.
- **Buscador de Flota**: decía "placa o conductor" pero no buscaba por conductor. Ahora busca por moto, placa, chofer o teléfono.
- **Inicio**: "motos cobradas de X" contaba motos sin chofer; "Gastos del mes" usaba el día de registro en vez de la fecha del gasto.
- **Copia en Drive**: un cambio hecho justo antes de cerrar la app no llegaba a la copia. Ahora se guarda al salir de la app (y no se reescribe si nada cambió).
- **Ícono de la app dañado** en Android 7 (los .webp estaban corruptos): se regeneraron desde el diseño original.
- Taller y compras se guardan en una sola operación; cuotas en 0 o negativas no se aceptan.
- Faltaba un import (icono Eliminar) que impedía compilar la v1.2/v1.3.

### Funciones nuevas
- **Corregir errores sin borrar la moto** (solo administrador):
  - Taller → Trabajos → "Anular": borra el trabajo, devuelve el dinero a Caja y el aceite/llantas al almacén.
  - Caja → Historial → tocar un movimiento: ver el detalle completo y anularlo. Una cuota anulada queda como no pagada.
  - Pagos → Deudas → tocar un cargo/abono: anularlo y la deuda se corrige sola (un abono anulado sale de Caja).
- **Pago incompleto**: si el chofer paga S/ 20 de S/ 35, al registrar el cobro se pueden pasar los S/ 15 que faltan a su deuda.
- **Chofer en la ficha de la moto**: nombre, teléfono y deuda a la vista, y botón **Estado de cuenta** del mes (días pagados, días sin pago, deuda, garantía), que se puede compartir. Sin WhatsApp ni llamadas.
- **Fechas automáticas y con calendario**: ya no se escriben fechas a mano. Se toca el campo y se elige el día en un calendario. El **fin del contrato se calcula solo** (inicio + modalidad: 1, 3, 6 meses o 1 año; ej. 05 nov 2026 + 3 meses = 05 feb 2027) y el **vencimiento del SOAT** es inicio + 1 año. Al cambiar el inicio o la modalidad, el fin se recalcula. Al **renovar**, se elige la modalidad y el contrato nuevo va desde el fin del anterior (u hoy si ya venció) hasta la fecha calculada; la modalidad queda guardada.
- **Reporte del mes para Excel**: Caja → Resumen → "Enviar reporte". Resumen por moto y todos los movimientos.
- **Cobro diario**: las motos en taller dicen "EN TALLER" en vez de "PENDIENTE", y se muestra lo que realmente pagó cada chofer.
- **Aviso de pocas llantas** en el almacén (además del de aceite).
- **Guía con gestos actualizada**: incluye las funciones nuevas. Quien ya hizo la guía antes verá una sola vez solo las **NOVEDADES** al actualizar; los cobradores no ven los pasos de administrador.

### Verificación
- Compilada con Gradle 9.3.1 y el SDK de Android 36.1: APK debug y release generados.
- Android Lint sin errores. 64 pruebas automáticas pasan.
- Versión 5 / 1.4. Sin cambios en la base de datos (sigue en la versión 7): actualizar no toca los datos.

## Cambios de la versión 1.3 (revisión de errores, 5 nov 2026)

- **Cobro directo duplicaba el ingreso**: al registrar un pago desde Flota/Caja se guardaba la cuota y además un "Pago Alquiler", así Caja mostraba el doble. Ahora es un solo registro con el monto real cobrado.
- **"Marcar todas" cobraba motos inactivas o en taller** y sobrescribía pagos ya registrados con otro monto. Ahora solo marca las que trabajan y faltan pagar; "Marcar todas" y "Desmarcar" piden confirmación.
- **Renovar contrato**: la fecha por defecto era fija ("15 Nov 2026") y en un formato que las alertas no entendían, así el contrato renovado nunca avisaba al vencer. Ahora sugiere la fecha según la modalidad (1 mes, 3 meses…), tiene botones +1/+3/+6 meses/+1 año, acepta 31/12/2026 o 2026-12-31, actualiza el inicio del contrato y no saca a la moto del taller.
- **Deudas**: el cargo y el abono leen el saldo actual de la base (antes un cargo seguido de un abono podía borrar el cargo) y se guardan en una sola operación. No se acepta un abono mayor que la deuda ni montos en 0.
- **Programar mantenimiento**: ya no trae datos de ejemplo (fecha 2026-09-20, S/ 45); empieza con la fecha de hoy y campos vacíos.
- Versión 4 / 1.3. Sin cambios en la base de datos (sigue en la versión 7): actualizar no toca los datos.

## Cambios de la versión 1.2 (observaciones del cliente, 1 nov 2026)

1. **Eliminar una moto.** Flota › tocar la moto › *Eliminar moto* (solo Administrador). La
   ventana avisa cuántos pagos, trabajos de taller y movimientos de caja tiene, porque se borran
   también (incluida la garantía que entró a Caja al registrarla). Si tiene historial, sugiere
   marcarla como Inactiva. Antes de borrar se guarda una copia interna completa.
2. **Moto inactiva no genera deuda.**
   - Al pasarla a *Inactivo* (Editar o Finalizar contrato) se anula la garantía que faltaba
     pagar. Queda en el historial como "Anulado" y no entra dinero a Caja. Otras deudas reales
     (cuotas no pagadas, daños) se conservan.
   - A una moto inactiva no se le pueden cargar deudas nuevas.
   - Las motos inactivas ya no aparecen en el cobro diario ni cuentan en "lo esperado".
   - Registrar una moto **sin chofer** no cobra garantía ni registra deuda.
3. **Editar nombre y placa** desde *Editar moto*. El cambio se aplica a todo su historial
   (pagos, deudas, taller, gastos, caja) en una sola operación. No se permiten placas repetidas,
   tampoco al registrar una moto nueva (antes sí, y mezclaba los datos de dos motos).
4. **Ordenar la flota** por Nombre, Placa, Estado (lo urgente primero) o Deuda (mayor primero).
   La app recuerda la opción. El orden por nombre es "humano": Moto 2 va antes que Moto 10. Pagos
   y Deudas también salen en ese orden. Se agregó el filtro *Inactivo*.

Sin cambios en la estructura de la base de datos: se instala encima de la 1.1 sin conversión.
46 pruebas automáticas; las consultas de renombrar y eliminar se probaron en SQLite real.

## Dónde quedan guardados los datos (un solo celular)

1. **En el teléfono** (base de datos de la app). Se usa siempre, funciona sin internet.
2. **Archivo en Google Drive** del dueño: `MotoFlota_respaldo.json`, creado con el botón
   "Guardar copia en Google Drive". La app lo actualiza unos segundos después de cada cambio y
   cada tarde a las 19:30, y después de escribirlo lo vuelve a leer para confirmar que quedó bien.
   Se ve en drive.google.com. (Se usa un ARCHIVO y no una carpeta porque Google Drive no deja
   que otras apps elijan carpetas suyas, pero sí crear y actualizar archivos.)
3. **Copia de seguridad de Android** en la cuenta de Google del teléfono (activada en v1.1):
   respalda la base de datos y los ajustes ~1 vez al día (con wifi y cargando). Si se reinstala
   la app con la misma cuenta, **los datos vuelven solos**. Con bloqueo de pantalla, Google la
   guarda cifrada de extremo a extremo. Para comprobarla: Ajustes del teléfono › Google ›
   Copia de seguridad.
   Tras restaurar en otro teléfono, la app vuelve a pedir la carpeta de Drive (ese permiso es
   de cada teléfono y no viaja en la copia).

## Auditoría de privacidad y funcionamiento (v1.1)
- La app **no tiene permiso de internet**: no puede enviar datos a ningún servidor. Los datos
  solo salen por Google Drive o por el correo que el dueño elige.
- **Notificaciones privadas:** con el teléfono bloqueado solo dice "Tienes avisos nuevos"; los
  nombres y montos se ven al desbloquear. Se quitó la notificación a pantalla completa.
- **Bloqueo automático:** tras 5 minutos fuera de la app, pide el PIN otra vez.
- **Sin copias sueltas:** los adjuntos de correo se borran del caché; en la memoria interna solo
  quedan las 4 copias de seguridad más recientes.
- Sin registros (logs) con datos, sin datos personales de prueba, componentes no expuestos.
- **Errores de comunicación corregidos:**
  - El adjunto del correo perdía su permiso de lectura (Gmail no podía leerlo).
  - La alerta "Revisión técnica del mes" llevaba a Taller, donde no se podía marcar; ahora abre
    la ventana de la moto donde está la casilla.
  - La revisión técnica mensual nunca volvía a quedar pendiente al cambiar de mes; ahora se
    reinicia el día 1 (también si la app no se abre, con el aviso de la mañana).
  - Resolver una alerta o registrar un trabajo de taller dejaba "pegada" la moto y su ficha se
    abría sola al entrar a Flota.
  - No había forma de silenciar los avisos: Menú › Silenciar avisos.
  - 15 campos de dinero no aceptaban coma ("35,50"); ahora todos la aceptan.
- 41 pruebas automáticas en `app/src/test`.

## Cómo actualizar la app del cliente SIN perder datos (leer antes de cada entrega)

1. **Nunca desinstales la app del cliente.** Desinstalar borra la base de datos del teléfono.
   Instala la versión nueva *encima* (botón ▶ Run en Android Studio, o abrir el APK nuevo).
2. **Firma siempre con la misma llave.** Android solo acepta instalar encima si el APK nuevo está
   firmado con la misma llave que el anterior. La llave de *debug* es distinta en cada computadora:
   si compilas en otra PC, Android dirá "App no instalada" y no habrá forma de actualizar sin
   desinstalar. Antes de entregar al cliente, crea UNA llave de release
   (Build › Generate Signed App Bundle/APK › Create new) y guárdala junto con su contraseña en un
   lugar seguro (y una copia fuera de la PC). Úsala en todas las entregas.
3. **Sube el número de versión** (`versionCode` en `app/build.gradle.kts`). Esta entrega es la 2 (v1.1).
4. **Antes de actualizar**, en el teléfono del cliente: Menú › Respaldos › *Copiar ahora*.
5. **Después de actualizar**, revisa que estén las motos, pagos y taller.

Red de seguridad incluida en la app:
- Al abrir una versión nueva, la base de datos del teléfono se **copia tal cual** antes de
  convertirla (`files/db_antes_de_actualizar/`, se guardan las 3 últimas). Si una actualización
  fallara, los datos siguen ahí y se recuperan con Android Studio › Device Explorer.
- Las conversiones de base de datos solo **agregan** columnas; nunca borran tablas. Si una fallara,
  la app no abre pero **los datos no se tocan** (se deshace todo).
- Recuperar una copia es **todo-o-nada**: si algo falla a mitad, los datos quedan como estaban.
- Antes de recuperar una copia se guarda lo que había en el teléfono, por si se eligió la equivocada.
- Se rechazan copias vacías (sin motos), que antes borraban todo.

### Prueba de actualización recomendada (en el emulador)
1. Instala la versión anterior (la que tiene el cliente) y registra 2 motos, pagos y un trabajo de taller.
2. Sin desinstalar, instala esta versión con ▶ Run.
3. Debe abrir con todo igual. Registra un trabajo con "Cables de cambios": debe sumarse al total.
4. Menú › Respaldos › Copiar ahora › Recuperar datos › De la carpeta: debe quedar igual.

## Inicio con cuenta de Google (evaluación)
- **Hecho:** en la configuración inicial, el botón "Usar mi cuenta de Google" toma el correo de la
  cuenta ya iniciada en el teléfono con un toque (sin escribirlo y sin contraseñas).
- **Inicio de sesión completo con Google** (entrar con la cuenta en vez del PIN): es posible, pero
  exige crear un proyecto en Google Cloud/Firebase con la cuenta del dueño y registrar la huella
  (SHA-1) de la llave de firma. Por sí solo no protege más los datos: tiene sentido cuando se haga
  la **sincronización entre varios celulares con Firebase**, que además guarda todo en la nube en
  tiempo real. Recomendación: hacer ambas cosas juntas en una siguiente etapa.

## Cambios de la versión 1.1
- Taller y Llantas: los textos y números ya no se parten en varias líneas en pantallas angostas o
  con letra grande; las tarjetas de inventario usan dos filas. Se revisaron todas las pantallas
  con el mismo problema (9 filas corregidas).
- Mantenimiento: nueva opción **Cables de cambios** (debajo de Planchado) con su costo.
- **Error corregido:** al registrar llantas en un trabajo de taller, su costo se sumaba dos veces
  en el total y en la Caja (2 llantas de S/ 85 descontaban S/ 340). Los registros anteriores a esta
  versión pueden tener ese doble cobro.
- **Error corregido:** las copias no guardaban la "revisión técnica del mes" de cada moto ni los
  lotes de compra de aceite/llantas; al recuperar una copia se perdían. Ahora se guardan.
- Los montos aceptan coma o punto ("35,50").

## Cambios de esta versión: vista más limpia, taller en un solo lugar, guía con gestos

- **Copia obligatoria antes de empezar.** Al primer ingreso del administrador aparecen 2 pasos con
  botones grandes: su correo y la carpeta de copias (recomendado: una carpeta "MotoFlota" dentro de
  Google Drive, así las copias quedan en su cuenta de Google). Si la carpeta ya tiene una copia
  (teléfono nuevo o prueba desde cero), se ofrece "Recuperar mis datos" con un toque.
- **Respaldos en una sola vista.** Estado ("Copia al día · última: hoy 16:45") y 3 botones:
  Copiar ahora, Enviar a mi correo y Recuperar datos (de la carpeta o de un archivo, por ejemplo el
  adjunto del correo). Se quitó el texto largo, el pegado de código JSON y la copia "interna".
- **Empezar de cero (pruebas).** Menú › Empezar de cero: pide el PIN del administrador y borra
  motos, pagos, caja, taller, usuarios y ajustes del teléfono. Las copias de la carpeta NO se borran.
- **Taller solo en Taller.** Se quitó "Ingresar moto al taller" y el registro de trabajos de la ficha
  de Flota, y los costos de taller de la Caja. La pestaña Taller tiene dos partes: *Trabajos*
  (registrar, motos en el taller con "Lista ✓", "+ Enviar moto", historial) y *Repuestos*
  (aceite, llantas e inventario).
- **Caja en dos partes:** *Resumen* (saldo, cobro/gasto, gráfico) e *Historial* (calendario y
  movimientos).
- **Historial y placas más livianos.** Las placas ya no se apilan letra por letra; cada movimiento
  ocupa 2 líneas cortas (concepto / fecha · tipo · placa). Lo mismo en el historial de deudas.
- **Guía con gestos.** En vez de párrafos y un "guiado obligatorio", la pantalla se oscurece, se
  ilumina el botón a usar y una mano animada lo toca, con una frase de pocas palabras. Cambia sola
  de pestaña (Inicio → Pagos → Flota → Taller → avisos → menú). "Saltar guía" la cierra; se repite
  desde Menú › Guía de uso.

## Versión anterior: Cambios de esta versión: diseño, alertas automáticas y usuarios

- **Inicio rediseñado.** Una tarjeta con los cobros de hoy (barra de avance), una sola lista de
  "Pendientes" (antes las alertas salían repetidas en 3 tarjetas), cifras compactas y accesos rápidos.
- **Barra superior simple.** Títulos cortos (Inicio, Pagos, Flota, Caja, Taller) y un único menú en
  el círculo con tu inicial (antes había 5 íconos sueltos y títulos largos que no cabían).
- **Letra simplificada.** De 14 tamaños escritos a mano (9 a 30) a una escala de 4 tamaños en
  `ui/theme/Type.kt` (`AppText`). La letra más pequeña pasó de 9–11 a 12 para leerse mejor. Para
  agrandar o achicar toda la app basta con cambiar esos 4 valores.
- **Alertas sin botones.** Se quitaron "Avisar al celular", "Revisar alertas" y similares. Tocar una
  alerta abre directamente lo que hay que hacer (SOAT/contrato → editar fechas; deuda → registrar
  abono; stock/revisión → Taller). La X la oculta hasta mañana.
- **Avisos automáticos.** Cada alerta nueva suena sola en el celular, una vez por día (al abrir la
  app o con los avisos de las 8:00 y 19:30), sin repetirse cada vez que cambia algo.
- **Sincronización automática.** Si configuraste la carpeta de sincronización, el respaldo se guarda
  solo unos segundos después de cada cambio (además del respaldo diario de las 19:30). Nunca se
  guarda un respaldo vacío, para no pisar el bueno al restaurar en un teléfono nuevo.

### Errores corregidos en esta versión
- La lista de motos del Inicio, del checklist de cobros y de las alertas era la lista **filtrada**
  por la búsqueda de la pestaña Flota: si buscabas una placa, el Inicio contaba mal y las alertas
  de las demás motos desaparecían. Ahora solo la lista de Flota usa el filtro.
- La tarjeta "Egresos Mes" sumaba **todo el historial**, no el mes. Ahora es "Gastos del mes" real.
- La tarjeta de cobros de hoy mostraba la fecha elegida en Pagos; ahora siempre es hoy.
- Las alertas ocultas volvían a aparecer al reabrir la app; ahora quedan ocultas hasta el día siguiente.
- El modo oscuro se perdía al cerrar la app; ahora se recuerda.

### Pruebas automáticas
`app/src/test` tiene 27 pruebas (usuarios, PIN, alertas, fechas, memoria de avisos). En Android
Studio: clic derecho en la carpeta `test` › **Run Tests**.

## Sincronizar entre varios teléfonos (no incluido)
Hoy cada teléfono tiene su propia base de datos. La carpeta de sincronización sirve para respaldar
y para pasar los datos a un teléfono nuevo, **no** para que dos personas registren a la vez en dos
celulares: si ambos escriben en la misma carpeta, uno borraría lo del otro. Para eso hace falta un
servidor (por ejemplo Firebase, con plan gratuito) que el dueño debe crear con su cuenta de Google.

## Pendiente recomendado (no incluido; requiere compilar y probar en Android Studio)
- Modelo de datos: relacionar tablas por ID en vez de placa, fechas como fecha real, entidad Chofer,
  no guardar valores calculados. Necesita una migración de Room con pruebas.
- Dividir `FleetViewModel.kt` (1.200 líneas) en un ViewModel por pantalla e incorporar Hilt.
- Cambiar tamaños de letra escritos a mano por el tema de Material 3.
- Activar `isMinifyEnabled` en release y probar el APK resultante.
- Cambiar `applicationId` y el paquete `com.example` por los definitivos antes de publicar en Play Store.
