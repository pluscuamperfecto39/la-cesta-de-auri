# La Cesta de Auri

Aplicación de lista de la compra para Android y iPhone, creada para funcionar sin cuentas, anuncios ni conexión a internet.

## Funciones

- Cesta actual con categorías, cantidades, progreso y productos tachables.
- Productos frecuentes aprendidos automáticamente al completar compras.
- Lista «Tengo que comprar» con fecha, hora y notificación opcional.
- Recuperación de recordatorios después de reiniciar el móvil.
- Envío de la cesta como texto mediante WhatsApp, correo u otras aplicaciones.
- Exportación de listas `.auri` para compartirlas entre Android y iPhone.
- Datos guardados únicamente en el dispositivo.

## Instalar el APK

El archivo terminado está en `dist/La-Cesta-de-Auri-v1.5.0.apk`. En el móvil, abre el archivo y autoriza temporalmente la instalación desde esa fuente si Android lo solicita. En Android 13 o posterior, acepta el permiso de notificaciones para recibir recordatorios.

## Proyectos

### Android

- Paquete: `com.auri.cesta`
- Android mínimo: 6.0 (API 23)
- Android objetivo: API 36
- Versión: 1.5.0
- Lenguaje: Java, sin servicios externos

Se incluye Gradle Wrapper para abrir y compilar el proyecto con Android Studio o desde terminal.

### iPhone

El proyecto nativo SwiftUI está en `ios/LaCestaDeAuri.xcodeproj` y admite iOS 16 o
posterior. Incluye las mismas tres secciones, notificaciones locales y compatibilidad
con los archivos `.auri` creados en Android. Consulta `ios/README.md` para abrirlo,
firmarlo e instalarlo desde un iMac con Xcode.

## Formato compartido

La especificación del archivo y una lista de ejemplo están en `docs/`. No hace falta
un servidor: se puede enviar el archivo por WhatsApp, Mail, AirDrop u otra aplicación
y la persona que tenga La Cesta de Auri podrá importarlo.
