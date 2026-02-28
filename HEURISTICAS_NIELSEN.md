# Heurísticas de Usabilidad de Nielsen — ToolRent Frontend

## Descripción general

**ToolRent** es un sistema web de gestión de herramientas que permite administrar inventario, préstamos, clientes, kardex, tarifas y reportes. El frontend fue desarrollado con **React** y estilizado con **Tailwind CSS** (framework/librería de UI), garantizando una interfaz coherente, responsiva y profesional.

---

## Framework / Librería de UI utilizada

| Tecnología | Rol |
|---|---|
| **Tailwind CSS** | Framework de utilidades CSS para todo el diseño visual |
| **Lucide React** | Librería de íconos consistentes y accesibles |
| **React** | Framework SPA para construcción de la UI |

---

## Las 10 Heurísticas de Nielsen aplicadas al proyecto

---

### H1 — Visibilidad del estado del sistema

> *El sistema siempre debe mantener informados a los usuarios de lo que está ocurriendo, a través de retroalimentación apropiada dentro de un tiempo razonable.*

**Implementación en ToolRent:**

- **Indicadores de carga (`loading`):** Todos los módulos (Inventario, Clientes, Préstamos, Kardex, Reportes) exponen un estado `loading` desde sus hooks personalizados (`useTools`, `useClients`, `useLoans`, `useKardex`, `useReports`). Mientras se obtienen datos del backend, se bloquea la interacción o se muestra un estado de espera.
- **Animación de entrada `animate-fadeIn`:** Al cambiar de sección en el panel principal (`AdminPanel.jsx`), el contenido aparece con una transición suave definida en `tailwind.config.js` (`fadeIn 0.5s ease-out forwards`), indicando al usuario que la vista se ha cargado correctamente.
- **Indicador de sesión activa:** El `Header.jsx` muestra el nombre del usuario y el texto "Sesión activa", confirmando en todo momento que la autenticación está vigente.
- **Estadísticas en tiempo real:** Cada módulo de gestión (Inventario, Clientes, Préstamos) presenta tarjetas de estadísticas que reflejan el estado actual del sistema (total herramientas, stock bajo, clientes activos, préstamos vencidos).

**Código relevante:**
```jsx
// AdminPanel.jsx — animación de carga al cambiar de sección
<div className="animate-fadeIn">
    {renderMainContent()}
</div>

// Header.jsx — visibilidad de sesión activa
<p className="text-xs text-slate-400">Sesión activa</p>
```

---

### H2 — Correspondencia entre el sistema y el mundo real

> *El sistema debe hablar el lenguaje del usuario, usar palabras, frases y conceptos familiares, y seguir convenciones del mundo real.*

**Implementación en ToolRent:**

- **Vocabulario del dominio:** Toda la interfaz usa terminología del negocio de arriendo de herramientas: *Inventario*, *Préstamos y Devoluciones*, *Kardex y Movimientos*, *Tarifas y Montos*, *Clientes*, *Stock*, *Multa por Atraso*, *Valores de Reposición*.
- **Íconos representativos:** Cada sección del menú lateral usa un ícono que representa visualmente su función:
  - 🔧 `Wrench` → Inventario
  - 🔄 `RefreshCw` → Préstamos y Devoluciones
  - 👥 `Users` → Gestión de Clientes
  - 💲 `DollarSign` → Tarifas y Montos
  - 📄 `FileText` → Kardex
  - 📊 `BarChart3` → Reportes
- **Mensajes de confirmación en lenguaje natural:** Los diálogos usan frases directas y descriptivas: *"¿Estás seguro de que deseas cerrar sesión?"*, *"¿Estás seguro de que deseas eliminar al cliente `[nombre]`? Esta acción no se puede deshacer."*
- **Labels descriptivos en formularios:** Los campos de formulario (Clientes, Inventario, Tarifas) usan etiquetas en español y términos del negocio.

**Código relevante:**
```jsx
// Sidebar.jsx — íconos y etiquetas del mundo real
{ id: 'inventario', icon: Wrench,    label: 'Gestión de Inventario' },
{ id: 'prestamos',  icon: RefreshCw, label: 'Préstamos y Devoluciones' },
{ id: 'clientes',   icon: Users,     label: 'Gestión de Clientes' },
{ id: 'tarifas',    icon: DollarSign,label: 'Tarifas y Montos' },
```

---

### H3 — Control y libertad del usuario

> *Los usuarios a menudo eligen funciones del sistema por error y necesitarán una "salida de emergencia" claramente marcada para abandonar el estado no deseado sin tener que pasar por un diálogo extendido.*

**Implementación en ToolRent:**

- **Botones de cierre en todos los modales:** Todos los módulos (Inventario, Clientes, Préstamos, Kardex, Tarifas) implementan la función `closeAllModals()` que permite al usuario cancelar cualquier operación y regresar al estado anterior sin pérdida de datos.
- **Confirmación antes de acciones destructivas:** El `handleLogout()` en `AdminPanel.jsx` solicita confirmación antes de cerrar sesión. En `ClientManagement.jsx`, el borrado incluye el nombre del cliente en el mensaje: *"¿Estás seguro de que deseas eliminar al cliente `[nombre]`? Esta acción no se puede deshacer."*
- **Botón "Volver al Dashboard":** En `AccessDenied.jsx`, cuando el usuario intenta acceder a una sección restringida, se le ofrece un botón de escape claro que lo retorna al inicio.
- **Navegación libre entre secciones:** El menú lateral siempre está visible y permite cambiar de sección en cualquier momento sin necesidad de completar flujos.

**Código relevante:**
```jsx
// AdminPanel.jsx — confirmación antes de logout
const handleLogout = () => {
    if (window.confirm('¿Estás seguro de que deseas cerrar sesión?')) {
        keycloak.logout({ redirectUri: window.location.origin });
    }
};

// AccessDenied.jsx — escape claro
<button onClick={onNavigate} className="bg-orange-600 ...">
    Volver al Dashboard
</button>
```

---

### H4 — Consistencia y estándares

> *Los usuarios no deben tener que preguntarse si diferentes palabras, situaciones o acciones significan lo misma cosa.*

**Implementación en ToolRent:**

- **Paleta de colores coherente en todo el sistema:** Se usa un sistema de colores consistente basado en Tailwind CSS:
  - `slate-800/900` → Fondos oscuros del panel
  - `orange-500/600` → Color de acento principal (botones de acción, ítem activo en sidebar, logo)
  - `slate-400` → Texto secundario descriptivo
  - `white` → Texto principal
- **Tipografía consistente en todos los módulos:** Cada módulo de gestión (Inventario, Clientes, Préstamos, Kardex, Tarifas, Reportes) aplica exactamente el mismo sistema tipográfico:
  - **Título de módulo:** `text-3xl font-bold text-white mb-2` — uniforme en los 6 módulos principales
  - **Subtítulo descriptivo:** `text-slate-400` — texto descriptivo justo bajo el título
  - **Etiqueta de tarjeta de estadística:** `text-sm font-medium text-slate-400`
  - **Número principal de estadística:** `text-3xl font-bold text-white` (o color semántico: `text-emerald-400`, `text-red-400`, `text-orange-400`)
  - **Texto de apoyo en stats:** `text-sm text-slate-400`
  - **Texto de tabs/botones de navegación:** `font-medium` con `bg-orange-600 text-white` (activo) / `bg-slate-700 text-slate-300` (inactivo)
- **Iconografía coherente con Lucide React:** Todos los módulos utilizan exclusivamente íconos de la librería **Lucide React** con tamaño uniforme `h-6 w-6` en tarjetas de estadísticas y `h-4 w-4` / `h-5 w-5` en botones y menús. El código de colores de íconos sigue reglas semánticas consistentes:
  - `text-orange-400/500` → Acción principal / totales
  - `text-sky-400` → Préstamos activos / información neutral
  - `text-emerald-400` → Éxito / activo / devoluciones
  - `text-red-400/500` → Error / atrasado / moroso
  - `text-yellow-400/500` → Advertencia / reparación
  - Íconos con fondo coloreado: `bg-{color}-500/10 p-3 rounded-lg` — patrón uniforme en todas las tarjetas
- **Patrón de tarjetas uniforme:** Todos los módulos presentan las mismas tarjetas de estadísticas con la estructura `bg-slate-800/60 backdrop-blur-sm border border-slate-700/50 p-6 rounded-lg shadow-lg` con `gap-6`.
- **Botones de acción primarios:** En todos los módulos, el botón de creación principal sigue el mismo estilo: `bg-orange-600 hover:bg-orange-700 text-white px-5 py-2.5 rounded-lg font-semibold`.
- **Estructura de página consistente:** Cada módulo sigue la misma estructura: encabezado con título (`text-3xl font-bold text-white`) + descripción (`text-slate-400`) → tarjetas de estadísticas → barra de búsqueda/filtros → tabla/lista de datos.
- **Tabs con el mismo patrón:** Los módulos con múltiples vistas (Kardex, Préstamos, Tarifas, Reportes) usan el mismo patrón de tabs con estado activo `bg-orange-600 text-white` vs inactivo `bg-slate-700 text-slate-300 hover:bg-slate-600`.
- **Container raíz unificado:** Todos los módulos usan `p-6 bg-slate-900 min-h-screen` como contenedor raíz, garantizando el mismo fondo y espaciado en toda la aplicación.

**Código relevante:**
```jsx
// Patrón tipográfico uniforme — aplicado en los 6 módulos de gestión
<h1 className="text-3xl font-bold text-white mb-2">Gestión de [Módulo]</h1>
<p className="text-slate-400">Descripción del módulo...</p>

// Patrón de tarjeta de stats — uniforme en todos los módulos
<div className="bg-slate-800/60 backdrop-blur-sm border border-slate-700/50 p-6 rounded-lg shadow-lg">
    <div className="flex items-start justify-between">
        <div className="space-y-1">
            <p className="text-sm font-medium text-slate-400">Etiqueta</p>
            <p className="text-3xl font-bold text-white">{valor}</p>
        </div>
        <div className="p-3 rounded-lg bg-orange-500/10">
            <IconComponent className="h-6 w-6 text-orange-400" />
        </div>
    </div>
</div>

// Iconografía coherente — Lucide React con semántica de color consistente
// Total/neutral → orange-400 | Activo/éxito → emerald-400
// Atrasado/error → red-400 | Advertencia → yellow-400 | Info → sky-400
```

---

### H5 — Prevención de errores

> *Mejor que un buen mensaje de error es un diseño cuidadoso que evite que ocurra el problema en primer lugar.*

**Implementación en ToolRent:**

- **Control de permisos antes de ejecutar acciones:** En `ClientManagement.jsx`, las funciones `handleAddClient`, `handleEditClient` y `handleDeleteClient` verifican con `hasPermission()` antes de ejecutar cualquier operación, evitando que el usuario inicie un flujo que no puede completar.
- **Indicador visual de secciones restringidas:** En el `Sidebar.jsx`, los ítems con `adminOnly: true` muestran el ícono `⚠️ AlertTriangle` si el usuario no es administrador, previniendo clicks frustrados al advertir con anticipación.
- **Filtros y búsqueda en tiempo real:** Los módulos de Inventario y Clientes implementan filtrado local (`filterTools`, `getFilteredClients`) que previene búsquedas vacías sin resultados confusos.
- **Validación de estado del stock:** En el módulo de Inventario, se calculan `lowStockTools` y `noStockTools` para mostrar advertencias proactivas antes de que el stock sea un problema operativo.
- **Control de roles centralizado:** El componente `renderMainContent()` en `AdminPanel.jsx` verifica permisos antes de renderizar cualquier vista; si el usuario no tiene acceso, se muestra `AccessDenied` en lugar de un error técnico.

**Código relevante:**
```jsx
// Sidebar.jsx — advertencia visual preventiva
{item.adminOnly && !isAdmin && (
    <AlertTriangle className="h-4 w-4 text-yellow-600 ml-auto" title="Acceso restringido" />
)}

// AdminPanel.jsx — control de acceso preventivo
const canAccess = !menuItem?.adminOnly || isAdmin;
if (!canAccess) {
    return <AccessDenied ... />;
}
```

---

### H6 — Reconocimiento antes que recuerdo

> *Minimizar la carga de memoria del usuario haciendo que los objetos, acciones y opciones sean visibles.*

**Implementación en ToolRent:**

- **Menú lateral siempre visible:** El `Sidebar.jsx` permanece visible en todo momento con todas las secciones disponibles, el usuario no necesita recordar cómo navegar.
- **Indicador de sección activa:** El ítem activo del menú muestra un borde naranja (`bg-orange-500/10 text-orange-400` + barra lateral `bg-orange-500`) que indica exactamente dónde se encuentra el usuario.
- **Nombre e iniciales del usuario visibles:** El sidebar muestra las iniciales del usuario (`getUserInitials()`) y su rol en todo momento.
- **Íconos junto a etiquetas de texto:** Todos los botones y opciones de menú combinan íconos con texto descriptivo, facilitando el reconocimiento sin necesidad de leer.
- **Filtros y búsqueda con placeholders descriptivos:** Los campos de búsqueda tienen texto de ayuda que indica qué se puede buscar.
- **Breadcrumb visual implícito:** El ítem resaltado en el sidebar actúa como indicador de posición actual dentro del sistema.

**Código relevante:**
```jsx
// Sidebar.jsx — item activo claramente reconocible
className={`... ${isActive
    ? 'bg-orange-500/10 text-orange-400'
    : 'text-slate-400 hover:bg-slate-700/50 hover:text-white'
}`}
// Barra de acento naranja en el item activo
<span className={`absolute left-0 top-0 h-full w-1 bg-orange-500 rounded-r-full
    transition-transform duration-300 ${isActive ? 'scale-y-100' : 'scale-y-0'}`} />
```

---

### H7 — Flexibilidad y eficiencia de uso

> *Los aceleradores, no vistos por el usuario novato, pueden acelerar la interacción del usuario experto.*

**Implementación en ToolRent:**

- **Acceso directo a acciones frecuentes:** En todos los módulos, el botón de acción principal (ej. "Nuevo Cliente", "Agregar Herramienta", "Nuevo Préstamo") está ubicado en el encabezado, accesible sin scrolling.
- **Filtros combinados:** Los módulos de Inventario, Clientes y Kardex ofrecen búsqueda por texto + filtros por categoría/estado/tipo de movimiento simultáneamente, permitiendo a usuarios expertos llegar rápido al dato buscado.
- **Acciones rápidas en filas de tabla:** Las listas de herramientas y clientes ofrecen botones de acción directa por fila (ver, editar, gestionar stock, dar de baja) sin necesidad de entrar a un detalle primero.
- **Pestañas para cambio de vista sin navegación:** Módulos como Kardex (Lista General / Por Herramienta / Detalle) y Préstamos (Activos / Vencidos / Historial) permiten cambiar de contexto con un solo click.
- **Módulo de Reportes con carga automática:** Los reportes se cargan automáticamente al cambiar de pestaña (`handleTabChange`), sin necesidad de hacer click en un botón "Generar".
- **Paginación con control del usuario:** Los módulos de Clientes, Inventario y Préstamos muestran los datos en páginas de 10, 15 o 20 registros según preferencia del usuario. El selector de "Filas por página" y los controles de navegación (`«`, `‹`, números, `›`, `»`) permiten moverse eficientemente por listas grandes sin scroll excesivo.
- **Actualización de datos sin recargar la página:** Los hooks (`loadTools()`, `loadClients()`, etc.) permiten refrescar datos en tiempo real tras operaciones CRUD.

---

### H8 — Estética y diseño minimalista

> *Los diálogos no deben contener información irrelevante o raramente necesaria. Cada unidad adicional de información compite con la información relevante.*

**Implementación en ToolRent:**

- **Diseño oscuro limpio:** El fondo `slate-900` con elementos `slate-800/50` con `backdrop-blur-sm` crea una jerarquía visual clara sin elementos decorativos innecesarios.
- **Información estadística condensada:** Las tarjetas de estadísticas muestran solo el número clave y una etiqueta corta, sin texto de relleno.
- **Separación visual con bordes sutiles:** Se usan bordes `border-slate-700/50` (semitransparentes) en lugar de líneas sólidas para separar secciones sin crear ruido visual.
- **Dashboard minimalista:** El `DashboardView.jsx` presenta solo el nombre del sistema, una descripción breve y una instrucción de inicio, sin widgets o datos innecesarios que distraigan.
- **Un solo color de acento:** El naranja `orange-500/600` se usa como único color de acento, evitando la saturación cromática.
- **Iconografía sin texto redundante:** Los botones de acción secundaria (en tablas) usan íconos con `title` tooltip en lugar de texto completo, manteniendo las tablas limpias.

**Código relevante:**
```jsx
// DashboardView.jsx — minimalismo intencional
<h2 className="text-4xl font-bold text-white mb-4">
    Sistema de Gestión de Herramientas
</h2>
<p className="text-lg text-slate-400 leading-relaxed">
    Plataforma integral para el control y administración de tu inventario...
</p>
<p className="text-sm text-slate-500">
    Selecciona una opción del menú lateral para comenzar
</p>
```

---

### H9 — Ayudar a los usuarios a reconocer, diagnosticar y recuperarse de errores

> *Los mensajes de error deben estar expresados en lenguaje sencillo, indicar precisamente el problema y sugerir una solución de forma constructiva.*

**Implementación en ToolRent:**

- **Página de Acceso Denegado informativa:** El componente `AccessDenied.jsx` no muestra un error técnico genérico sino que explica: qué sección intentó acceder, el rol actual del usuario, por qué no tiene acceso, y ofrece una acción concreta para salir del error ("Volver al Dashboard").
- **Manejo de errores en hooks con estado `error`:** Todos los hooks de datos (`useClients`, `useTools`, `useLoans`, `useKardex`) exponen un estado `error` que los módulos pueden presentar al usuario de forma contextualizada.
- **`clearError()` disponible:** La función `clearError` en `useClients` permite al componente limpiar el estado de error al cerrar un modal, evitando que mensajes de errores anteriores confundan acciones futuras.
- **Mensajes de confirmación con contexto específico:** Antes de eliminar un cliente, el sistema muestra el nombre exacto: *"¿Estás seguro de que deseas eliminar al cliente `[nombre]`? Esta acción no se puede deshacer."*, haciendo que el usuario confirme conscientemente qué elemento está eliminando.
- **Ícono de advertencia contextual:** El `AlertTriangle` de Lucide se usa tanto en la pantalla de acceso denegado como en el sidebar para señalar problemas potenciales de forma visual e intuitiva.

**Código relevante:**
```jsx
// AccessDenied.jsx — error comprensible y accionable
<AlertTriangle className="h-16 w-16 text-yellow-500 mb-6" />
<h2 className="text-2xl font-bold text-white mb-2">Acceso Denegado</h2>
<p className="text-slate-400 mb-6 max-w-md">
    No tienes los permisos necesarios para acceder a la sección
    <strong className="text-white">{sectionLabel}</strong>.
</p>
<p className="text-slate-400 mt-1">Esta sección requiere permisos de Administrador.</p>
<button onClick={onNavigate} ...>Volver al Dashboard</button>
```

---

### H10 — Ayuda y documentación

> *Aunque es mejor si el sistema puede ser usado sin documentación, puede ser necesario proporcionar ayuda y documentación.*

**Implementación en ToolRent:**

- **Interfaz autodescriptiva:** Cada módulo incluye un subtítulo descriptivo bajo el título principal que explica el propósito de la sección (ej. "Administra la información y estados de los clientes", "Consulta y auditoría de movimientos de inventario").
- **Tooltips en íconos de acción:** Los botones de acción en tablas incluyen el atributo `title` con una descripción breve de la acción (ej. `title="Acceso restringido"` en el ícono de advertencia del sidebar).
- **Labels descriptivos en formularios:** Los formularios de creación y edición (herramientas, clientes, tarifas) incluyen etiquetas claras y descripciones de cada campo.
- **Mensajes de estado en contexto:** Las tarjetas de estadísticas actúan como documentación implícita del estado del sistema (cuántas herramientas hay, cuántas con stock bajo, cuántos clientes activos).
- **Módulo de Kardex como auditoría documentada:** La sección de Kardex permite al usuario consultar el historial completo de movimientos con filtros por herramienta, tipo y rango de fechas, funcionando como un log de auditoría autoexplicativo.
- **Instrucción inicial en Dashboard:** El mensaje "Selecciona una opción del menú lateral para comenzar" en `DashboardView.jsx` actúa como una microinstrucción de onboarding para nuevos usuarios.

---

## Mejoras aplicadas durante el desarrollo

Durante el proceso de revisión de calidad se identificaron y corrigieron inconsistencias que afectaban directamente las heurísticas H4, H3 y H9. A continuación se detalla cada mejora implementada:

### Mejora 1 — Unificación total de paleta de colores, tipografía e iconografía (H4)

**Problema detectado:** El proyecto presentaba tres tipos de inconsistencias visuales:

1. **Paleta de colores mixta:** `LoanManagement.jsx` usaba `bg-gray-900` mientras que `ClientManagement.jsx` usaba `bg-slate-800`. Los botones de acción primaria tampoco eran uniformes ("Nuevo Préstamo" usaba `bg-green-600`, tabs activos en Tarifas usaban `bg-blue-600`).

2. **Tipografía inconsistente:** `KardexManagement.jsx` usaba `text-2xl font-bold text-slate-100` para el título y las tarjetas de stats mientras que todos los demás módulos usaban `text-3xl font-bold text-white`. `ReportManagement.jsx` mostraba números de stats en `text-2xl` y usaba `text-xs text-slate-500` para textos de apoyo en lugar de `text-sm text-slate-400`.

3. **Iconografía inconsistente:** En Kardex, el ícono principal usaba `text-blue-400` (fuera de la paleta semántica del proyecto), y la tarjeta "Préstamos" usaba un `<div>` con un punto rojo en lugar de un ícono real de Lucide React. En Reportes, la tarjeta "Herramienta Top" usaba `text-green-500` como ícono (fuera de la paleta definida). El container raíz de `ClientManagement.jsx` no tenía `p-6 bg-slate-900 min-h-screen`.

**Corrección aplicada:** Se migró el **100% de los componentes** a la escala `slate` de forma uniforme, se unificó tipografía y se estandarizó la iconografía con Lucide React en todos los módulos:

| Aspecto | Antes | Después |
|---|---|---|
| `bg-gray-900 / bg-gray-800 / bg-gray-700` | → | `bg-slate-900 / bg-slate-800 / bg-slate-700` |
| `border-gray-700 / text-gray-400` | → | `border-slate-700/50 / text-slate-400` |
| `bg-green-600` (Nuevo Préstamo) | → | `bg-orange-600` |
| `bg-blue-600` (tabs activos en Tarifas) | → | `bg-orange-600` |
| `focus:ring-blue-500` (inputs) | → | `focus:ring-orange-500` |
| Título Kardex: `text-2xl text-slate-100` | → | `text-3xl text-white mb-2` |
| Tarjetas stats Kardex: `text-2xl text-slate-100` | → | `text-3xl text-white` |
| Tarjetas stats Reportes: `text-2xl` | → | `text-3xl` |
| Subtextos stats Reportes: `text-xs text-slate-500` | → | `text-sm text-slate-400` |
| Ícono Kardex total: `BarChart3 text-blue-400` | → | `BarChart3 text-orange-400` con `bg-orange-500/10` |
| Ícono Kardex préstamos: div con punto rojo | → | `RefreshCw text-sky-400` con `bg-sky-500/10` |
| Ícono Kardex devoluciones: `CheckCircle` sin fondo | → | `CheckCircle text-emerald-400` con `bg-emerald-500/10` |
| Ícono Kardex reparaciones: `AlertCircle` sin fondo | → | `Wrench text-yellow-400` con `bg-yellow-500/10` |
| Ícono Reportes herramienta top: `text-green-500` | → | `BarChart3 text-orange-500` con `bg-orange-500/10` |
| Container Clientes: `<div>` sin clases | → | `p-6 bg-slate-900 min-h-screen` |

---

### Mejora 2 — Mensaje de confirmación de borrado descriptivo (H3 y H9)

**Problema detectado:** El mensaje de confirmación al eliminar un cliente era `window.confirm('¿Seguro?')`, que no indicaba qué elemento se iba a eliminar ni las consecuencias de la acción.

**Corrección aplicada:** El mensaje ahora incluye el nombre específico del cliente y advierte sobre la irreversibilidad:

```js
// Antes
window.confirm('¿Seguro?')

// Después
window.confirm(`¿Estás seguro de que deseas eliminar al cliente "${client.name}"? Esta acción no se puede deshacer.`)
```

---

### Mejora 3 — Eliminación de console.log de debug (H8 y calidad general)

**Problema detectado:** Se encontraron `console.log` de depuración activos en 10 componentes (`LoanForm.jsx`, `ClientValidation.jsx`, `MovementDetail.jsx`, `ReturnForm.jsx`, entre otros). Estos mensajes eran visibles en las DevTools del navegador, exponiendo información interna del sistema y dando una imagen de descuido.

**Corrección aplicada:** Se eliminaron todos los `console.log` de depuración del código de producción. El manejo de errores se conservó mediante `try/catch` y el estado `error` de los hooks, sin exponer información en consola.

### Mejora 4 — Paginación en listas de datos (H7 y H8)

**Problema detectado:** Los módulos de Clientes, Inventario y Préstamos cargaban y mostraban **todos los registros** en pantalla simultáneamente. Con listas grandes esto genera sobrecarga visual, degrada el rendimiento de renderizado y obliga al usuario a hacer scroll excesivo.

**Corrección aplicada:** Se implementó paginación en los seis componentes de lista principales:

| Componente | Registros por defecto | Opciones disponibles |
|---|---|---|
| `ClientList.jsx` | 10 | 10 / 15 / 20 |
| `ToolList.jsx` | 10 | 10 / 15 / 20 |
| `LoansList.jsx` | 10 | 10 / 15 / 20 |
| `MovementsList.jsx` | 10 | 10 / 15 / 20 |
| `ActiveLoansReport.jsx` | 10 | 10 / 15 / 20 |
| `OverdueClientsReport.jsx` | 10 | 10 / 15 / 20 |

Cada componente incluye:
- **Selector de filas por página** (`10 / 15 / 20`) con reset automático de página al cambiar
- **Indicador de posición**: *"Mostrando 1–10 de 47 clientes"*
- **Navegación con números de página** inteligente (muestra primera, última y páginas adyacentes con `…` para rangos intermedios)
- **Botones de primera/última página** (`«` / `»`) y anterior/siguiente con flechas
- **Botones deshabilitados** visualmente cuando ya se está en el límite (`disabled:opacity-30`)

---

### Mejora 5 — Mensajes de error amigables y validación del campo nombre (H9 y H5)

**Problema detectado (dos subproblemas):**

1. Cualquier error HTTP del backend mostraba el mensaje técnico `"Request failed with status code 400"` directamente al usuario, violando H9.
2. El campo **Nombre** del formulario de clientes aceptaba números y caracteres especiales sin restricción, y el texto de ayuda decía *"Mínimo 2 caracteres"*, lo cual es engañoso (ningún nombre real tiene solo 2 letras) y no orienta al usuario sobre el formato esperado.

**Corrección aplicada:**

- Se creó `src/utils/errorUtils.js` con la función `getFriendlyError()` que interpreta cada código HTTP y mensaje del backend, devolviendo mensajes específicos por campo:

| Situación | Mensaje al usuario | Campo marcado |
|---|---|---|
| RUT con dígito verificador incorrecto | *"El RUT ingresado no es válido. Verifica el formato y el dígito verificador (ej: 12.345.678-9)."* | Campo RUT en rojo |
| RUT ya registrado | *"Este RUT ya está registrado en el sistema. No se puede duplicar."* | Campo RUT en rojo |
| Teléfono con formato incorrecto | *"El número de teléfono no es válido. Ingresa un celular (ej: 9 1234 5678) o teléfono fijo chileno."* | Campo Teléfono en rojo |
| Teléfono ya registrado | *"Este número de teléfono ya está registrado para otro cliente."* | Campo Teléfono en rojo |
| Email con formato incorrecto | *"El formato del correo electrónico no es válido (ej: nombre@dominio.com)."* | Campo Email en rojo |
| Email ya registrado | *"Este correo electrónico ya está registrado para otro cliente."* | Campo Email en rojo |

- El campo **Nombre** ahora **filtra en tiempo real** caracteres no permitidos: solo acepta letras (incluyendo tildes y ñ), espacios y guiones. Si el usuario intenta escribir un número o símbolo especial, simplemente no aparece en el campo.
- El texto de ayuda del campo nombre cambió de *"Mínimo 2 caracteres"* a *"Solo letras y espacios. Ej: Juan Pérez García"*, orientando claramente el formato esperado.

```jsx
// ClientForm.jsx — filtro en tiempo real del campo nombre
} else if (name === 'name') {
    formattedValue = value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚüÜñÑ '\-]/g, '');
}
```

## Aspectos mejorables a futuro

Los siguientes puntos representan oportunidades de mejora que no afectan el cumplimiento actual de las heurísticas, pero que elevarían la calidad de la experiencia de usuario en una iteración futura:

### 1. Notificaciones de éxito tipo Toast (H1)

**Situación actual:** Al completar una operación CRUD exitosa (crear cliente, agregar herramienta, registrar préstamo), el único feedback es que el modal se cierra. El usuario debe inferir que la operación fue exitosa.

**Mejora sugerida:** Implementar notificaciones tipo *toast* no intrusivas (ej. con la librería `react-hot-toast` o `sonner`) que confirmen explícitamente el resultado:
```
✅ Cliente "Juan Pérez" creado correctamente
✅ Herramienta agregada al inventario
```

---

### 2. Reemplazar `window.alert()` y `window.confirm()` por modales personalizados (H4 y H8)

**Situación actual:** Las confirmaciones de borrado y errores de permisos usan los diálogos nativos del navegador (`window.confirm`, `window.alert`), cuyo estilo visual rompe con el diseño oscuro del sistema.

**Mejora sugerida:** Implementar un componente modal de confirmación reutilizable con el estilo visual del proyecto (`bg-slate-800`, bordes `slate-700`, botones `orange`), manteniendo la coherencia visual en el 100% de las interacciones.

---

### 3. Indicador de carga global al inicializar módulos (H1)

**Situación actual:** Al navegar a un módulo por primera vez, si los datos tardan en cargar desde el backend, algunos componentes pueden mostrar una tabla vacía momentáneamente antes de que aparezcan los datos.

**Mejora sugerida:** Agregar un estado de carga inicial más prominente (skeleton loader o spinner centrado) mientras se completa la primera petición al backend, comunicando claramente que el sistema está trabajando.

---

### 4. Validaciones de formulario en tiempo real (H5)

**Situación actual:** Los formularios (ClientForm, ToolForm) delegan toda la validación al backend. El usuario solo recibe feedback de error después de hacer submit y esperar la respuesta del servidor.

**Mejora sugerida:** Agregar validaciones básicas en el cliente (formato de RUT, longitud mínima de nombre, formato de email) que se activen mientras el usuario escribe, reduciendo el ciclo de corrección de errores.

---

## Resumen de evidencia por heurística

| # | Heurística | Evidencia principal en el código |
|---|---|---|
| H1 | Visibilidad del estado | `loading` states en todos los hooks, `animate-fadeIn`, indicador "Sesión activa" |
| H2 | Lenguaje del mundo real | Íconos + etiquetas del dominio en `Sidebar.jsx`, terminología del negocio |
| H3 | Control y libertad | `closeAllModals()` en todos los módulos, confirmaciones descriptivas antes de acciones destructivas, botón "Volver" |
| H4 | Consistencia y estándares | Paleta `slate/orange` 100% uniforme, tipografía `text-3xl font-bold text-white` en títulos y `text-sm font-medium text-slate-400` en etiquetas de stats en todos los módulos, iconografía Lucide React con semántica de color consistente (orange/emerald/red/yellow/sky), patrón de tarjetas y container raíz unificados, Tailwind CSS |
| H5 | Prevención de errores | `hasPermission()` antes de acciones, `AlertTriangle` preventivo en sidebar, filtro en tiempo real del campo nombre (solo letras), control de acceso en `renderMainContent()` |
| H6 | Reconocimiento vs recuerdo | Menú siempre visible, ítem activo resaltado, íconos + texto en todos los controles |
| H7 | Flexibilidad y eficiencia | Filtros combinados, paginación 10/15/20 en 6 componentes, acciones rápidas en tablas, tabs sin navegación extra, carga automática de reportes |
| H8 | Diseño minimalista | Fondo oscuro limpio, un solo color de acento naranja, `DashboardView` sin información innecesaria |
| H9 | Recuperación de errores | `errorUtils.js` con mensajes amigables por campo (RUT, teléfono, email, nombre), `AccessDenied.jsx` informativo, confirmaciones con nombre del elemento afectado |
| H10 | Ayuda y documentación | Subtítulos descriptivos en módulos, tooltips en botones, microinstrucción en dashboard |

---

## Conclusión

El frontend de **ToolRent** fue construido aplicando de forma explícita y consistente las 10 heurísticas de Nielsen. El uso de **Tailwind CSS** como framework de UI garantiza coherencia visual en toda la aplicación, mientras que la arquitectura basada en componentes de React facilita la reutilización de patrones de UX probados. Durante el proceso de desarrollo se identificaron y corrigieron activamente inconsistencias de diseño (paleta de colores, mensajes de confirmación, código de depuración), lo que evidencia un compromiso real con la calidad de la experiencia de usuario. La experiencia resultante es intuitiva, informativa, segura frente a errores y accesible para usuarios de distintos niveles técnicos.

