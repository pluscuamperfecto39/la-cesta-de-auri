# La Cesta de Auri para iPhone

Proyecto nativo para iPhone creado con SwiftUI. Comparte el mismo formato de listas
`.auri` que la versión Android, por lo que una lista exportada desde cualquiera de
los dos sistemas puede importarse en el otro.

## Abrir en el iMac

1. Instala la versión más reciente de Xcode disponible desde la App Store.
2. Clona este repositorio y abre `ios/LaCestaDeAuri.xcodeproj`.
3. Selecciona el proyecto **LaCestaDeAuri**, abre **Signing & Capabilities** y elige
   tu cuenta en **Team**. Si Xcode indica que el identificador ya existe, cambia
   `com.auri.cesta.ios` por uno propio, por ejemplo
   `com.tunombre.lacestadeauri`.
4. Conecta el iPhone, selecciónalo como destino y pulsa **Run**.

La app admite iOS 16 o posterior. Xcode puede compilarla tanto en el simulador como
en un iPhone. Para publicarla en App Store hace falta una cuenta de Apple Developer,
completar los datos de privacidad y generar el archivo desde **Product > Archive**.

## Qué ya está implementado

- Cesta con nombre, cantidad, categoría y productos tachables.
- Productos frecuentes aprendidos al marcar compras.
- Lista «Para luego» con fecha, hora y notificaciones locales.
- Importación desde Archivos y apertura directa de archivos `.auri`.
- Confirmación y validación antes de añadir una lista recibida.
- Exportación de la cesta como archivo `.auri` mediante la hoja de compartir de iOS.
- Persistencia local sin cuentas ni servicios externos.
- Diseño morado y celeste adaptado a las zonas seguras del iPhone.

## Compatibilidad de las listas

El tipo registrado en iOS es `com.auri.cesta.list`, con extensión `.auri` y MIME
`application/vnd.auri.cesta-list+json`. La especificación completa está en
`docs/FORMATO-AURI.md`.

## Límite de Windows

El código y el proyecto Xcode se pueden preparar en Windows, pero Apple solo permite
compilar, firmar, instalar y publicar aplicaciones iOS desde macOS con Xcode. Por
eso el proyecto se ha validado estructuralmente aquí y la compilación final debe
hacerse en el iMac.
