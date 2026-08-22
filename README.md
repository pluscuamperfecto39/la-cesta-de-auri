# La Cesta de Auri

Aplicación Android de lista de la compra, creada para funcionar sin cuentas, anuncios ni conexión a internet.

## Funciones

- Cesta actual con categorías, cantidades, progreso y productos tachables.
- Productos frecuentes aprendidos automáticamente al completar compras.
- Lista «Tengo que comprar» con fecha, hora y notificación opcional.
- Recuperación de recordatorios después de reiniciar el móvil.
- Envío de la cesta como texto mediante WhatsApp, correo u otras aplicaciones.
- Exportación de listas `.auri` para compartirlas e importarlas en otro móvil con la app.
- Datos guardados únicamente en el dispositivo.

## Instalar el APK

El archivo terminado está en `dist/La-Cesta-de-Auri-v1.4.0.apk`. En el móvil, abre el archivo y autoriza temporalmente la instalación desde esa fuente si Android lo solicita. En Android 13 o posterior, acepta el permiso de notificaciones para recibir recordatorios.

## Proyecto

- Paquete: `com.auri.cesta`
- Android mínimo: 6.0 (API 23)
- Android objetivo: API 36
- Versión: 1.4.0
- Lenguaje: Java, sin servicios externos

Se incluye Gradle Wrapper para abrir y compilar el proyecto con Android Studio o desde terminal.
