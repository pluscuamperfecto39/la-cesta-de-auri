# Formato compartido `.auri`

`La Cesta de Auri` usa un documento JSON UTF-8 con extensión `.auri` para mover una
lista entre Android y iPhone sin una cuenta ni un servidor.

## Identificación

- Extensión: `.auri`
- MIME: `application/vnd.auri.cesta-list+json`
- UTI de iOS: `com.auri.cesta.list`
- Formato: `la-cesta-de-auri`
- Versión actual: `1`

## Esquema de la versión 1

```json
{
  "format": "la-cesta-de-auri",
  "version": 1,
  "createdAt": 1787392800000,
  "items": [
    {
      "name": "Tomates",
      "category": "Fruta y verdura",
      "quantity": "1 kg",
      "checked": false
    }
  ]
}
```

`createdAt` se expresa en milisegundos desde Unix Epoch. Al importar, los productos
se añaden como pendientes aunque estuvieran marcados en la cesta de origen.

## Categorías admitidas

- Fruta y verdura
- Despensa
- Lácteos
- Panadería
- Carne y pescado
- Bebidas
- Hogar
- Higiene
- Otros

Una categoría desconocida se convierte en `Otros`.

## Validación de seguridad

Las dos aplicaciones comprueban el identificador y la versión antes de leer los
productos. La versión iPhone limita el archivo a 512 KB, 200 productos, 100
caracteres por nombre y 40 por cantidad. Los archivos vacíos o mal formados no se
importan.
