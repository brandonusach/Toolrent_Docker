# 🚀 ToolRent - Guía de Despliegue con Docker

## 📋 Prerequisitos

- **Docker** y **Docker Compose** instalados
- **PostgreSQL** corriendo (puerto 5432)
- **Keycloak** corriendo (puerto 8080)
- Puertos disponibles: `8070` (frontend), `8081` (backend), `8080` (Keycloak), `5432` (PostgreSQL)

---

## 1️⃣ Configurar la IP del Host

Edita el archivo `.env` en la raíz del proyecto (`toolrent/.env`):

```env
HOST_IP=TU_IP_LOCAL
```

Para obtener tu IP local en **Windows PowerShell**:
```powershell
(Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.InterfaceAlias -notlike "*Loopback*" -and $_.PrefixOrigin -eq "Dhcp" }).IPAddress
```

O simplemente:
```powershell
ipconfig
```

> ⚠️ **Nota:** El `HOST_IP` aparece resaltado/rojo en el editor porque el IDE no resuelve las variables `${...}` de Docker Compose. **Esto es normal** y NO es un error. Docker Compose lee automáticamente el archivo `.env` al ejecutarse.

---

## 2️⃣ Levantar PostgreSQL (si no lo tienes)

### Opción A: Con Docker
```powershell
docker run -d `
  --name toolrent-postgres `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=1234 `
  -e POSTGRES_DB=toolrent_db `
  -p 5432:5432 `
  postgres:16
```

### Opción B: Verificar si ya está corriendo
```powershell
docker ps | Select-String "postgres"
```

---

## 3️⃣ Levantar Keycloak

### Iniciar Keycloak con Docker
```powershell
docker run -d `
  --name toolrent-keycloak `
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin `
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin `
  -p 8080:8080 `
  quay.io/keycloak/keycloak:26.2.0 start-dev
```

### Iniciar Keycloak desde instalación local (kc.bat)
```powershell
.\kc.bat start-dev --http-port=8080
```

### Verificar que Keycloak está corriendo
```powershell
# Espera ~30 segundos y verifica accediendo a:
# http://localhost:8080
# Usuario: admin / Contraseña: admin
```

---

## 4️⃣ Configurar Keycloak (Realm, Client y Roles)

Accede a la consola de administración: **http://localhost:8080/admin**

### 4.1 Crear el Realm
1. Click en el dropdown del realm (esquina superior izquierda, dice "master")
2. Click en **"Create realm"**
3. **Realm name:** `toolrent-realm`
4. Click **"Create"**

### 4.2 Crear el Client
1. En el menú lateral, ir a **Clients** → **Create client**
2. **Client type:** OpenID Connect
3. **Client ID:** `toolrent-frontend`
4. Click **Next**
5. **Client authentication:** OFF (es un client público)
6. **Standard flow:** ✅ Habilitado
7. **Direct access grants:** ✅ Habilitado
8. Click **Next**
9. Configurar **Access Settings** (las URLs):

| Campo | Valor |
|-------|-------|
| **Root URL** | `http://192.168.1.7:8070` |
| **Home URL** | `http://192.168.1.7:8070` |
| **Valid redirect URIs** | `http://192.168.1.7:8070/*` |
|  | `http://localhost:8070/*` |
| **Valid post logout redirect URIs** | `http://192.168.1.7:8070/*` |
|  | `http://localhost:8070/*` |
| **Web origins** | `*` |
| **Admin URL** | `http://192.168.1.7:8070` |

10. Click **Save**

> ⚠️ **IMPORTANTE:** Las URLs deben apuntar al **FRONTEND** (puerto `8070`), NO al backend (8081).  
> Si tu IP cambia, actualiza tanto el `.env` (`HOST_IP`) como estas URLs en Keycloak.  
> La IP `172.20.147.108:30081` que tenías antes es **INCORRECTA** — era una IP/puerto que no corresponden.

### 4.3 Crear los Roles del Realm
1. En el menú lateral, ir a **Realm roles** → **Create role**
2. Crear los siguientes roles (uno por uno):
   - **ADMINISTRATOR**
   - **EMPLOYEE**
3. Para cada uno: escribir el nombre del rol y click **Save**

### 4.4 Crear Usuarios de Prueba
1. En el menú lateral, ir a **Users** → **Add user**
2. **Usuario Administrador:**
   - **Username:** `admin`
   - **Email:** `admin@toolrent.com`
   - **First name:** `Admin`
   - **Last name:** `ToolRent`
   - **Email verified:** ✅ ON
   - Click **Create**
   - Ir a la pestaña **Credentials** → **Set password**
     - **Password:** `admin123`
     - **Temporary:** OFF
   - Ir a la pestaña **Role mapping** → **Assign role**
     - Seleccionar **ADMINISTRATOR** y **EMPLOYEE**
     - Click **Assign**

3. **Usuario Empleado (opcional):**
   - **Username:** `employee`
   - **Email:** `employee@toolrent.com`
   - **First name:** `Empleado`
   - **Last name:** `ToolRent`
   - **Email verified:** ✅ ON
   - Click **Create**
   - Ir a la pestaña **Credentials** → **Set password**
     - **Password:** `employee123`
     - **Temporary:** OFF
   - Ir a la pestaña **Role mapping** → **Assign role**
     - Seleccionar **EMPLOYEE**
     - Click **Assign**

---

## 5️⃣ Levantar la Aplicación con Docker Compose

### Construir y levantar todos los servicios
```powershell
cd C:\Users\brand\Desktop\Proyecto\toolrent
docker compose up --build -d
```

### Ver los logs en tiempo real
```powershell
docker compose logs -f
```

### Ver logs de un servicio específico
```powershell
docker compose logs -f backend1
docker compose logs -f frontend1
docker compose logs -f nginx-backend
docker compose logs -f nginx-frontend
```

### Verificar que todos los servicios están corriendo
```powershell
docker compose ps
```

---

## 6️⃣ Modo Desarrollo (Hot Reload)

> ✅ **Usa UNO u OTRO, nunca ambos al mismo tiempo.**  
> - ¿Estás **modificando código**? → usa el comando de **desarrollo**  
> - ¿Es la **entrega final**? → usa el comando de **producción**

---

### 🛠️ Mientras estás desarrollando (hot reload)

El frontend se actualiza automáticamente en el navegador cada vez que guardas un archivo. **No hay que reconstruir Docker.**

```powershell
# Primera vez (instala dependencias y levanta)
docker compose --profile dev up frontend-dev --build

# Las siguientes veces (solo levanta, sin reconstruir)
docker compose --profile dev up frontend-dev
```

Acceder en → **http://localhost:5173**

> ⚠️ El backend y Keycloak deben estar corriendo por separado (pasos 2, 3 y 5).

---

### 🚀 Entrega final (producción)

Construye y sirve todo optimizado con nginx.

```powershell
docker compose up --build -d
```

Acceder en → **http://localhost:8070**

---

| | Desarrollo | Producción |
|---|---|---|
| **Comando** | `docker compose --profile dev up frontend-dev` | `docker compose up --build -d` |
| **URL** | http://localhost:5173 | http://localhost:8070 |
| **Cambios de código** | ✅ Se reflejan al instante | ❌ Requiere `--build` |
| **Cuándo usarlo** | Mientras programas | Entrega final |

---

## 7️⃣ Acceder a la Aplicación

| Servicio | URL |
|----------|-----|
| **Frontend (app)** | http://localhost:8070 |
| **Backend API** | http://localhost:8081/api |
| **Keycloak Admin** | http://localhost:8080/admin |

---

## 8️⃣ Comandos Útiles

### Detener todos los servicios
```powershell
docker compose down
```

### Detener y eliminar volúmenes
```powershell
docker compose down -v
```

### Reconstruir un servicio específico
```powershell
docker compose up --build -d backend1
```

### Reiniciar todos los servicios
```powershell
docker compose restart
```

### Ver el estado de los contenedores
```powershell
docker compose ps
```

### Limpiar imágenes no utilizadas
```powershell
docker image prune -f
```

---

## 🔧 Troubleshooting

### Error: "Keycloak no responde"
```powershell
# Verificar que Keycloak está corriendo
docker ps | Select-String "keycloak"

# Ver logs de Keycloak
docker logs toolrent-keycloak --tail 50
```

### Error: "Cannot connect to PostgreSQL"
```powershell
# Verificar que PostgreSQL está corriendo
docker ps | Select-String "postgres"

# Probar conexión
docker exec -it toolrent-postgres psql -U postgres -d toolrent_db -c "SELECT 1"
```

### Error: "CORS" o problemas de red
- Verifica que `HOST_IP` en `.env` sea tu IP real (no `localhost` ni `127.0.0.1`)
- Verifica que los puertos no estén ocupados:
```powershell
netstat -ano | Select-String "8070"
netstat -ano | Select-String "8081"
netstat -ano | Select-String "8080"
```

### Reconstruir todo desde cero
```powershell
docker compose down -v
docker compose build --no-cache
docker compose up -d
```

---

## 📁 Arquitectura del Despliegue

```
                    ┌─────────────┐
                    │   Browser   │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
        :8070 │      :8081 │      :8080 │
    ┌─────────▼──┐  ┌──────▼─────┐  ┌──▼──────────┐
    │   NGINX    │  │   NGINX    │  │  Keycloak   │
    │ (Frontend) │  │ (Backend)  │  │             │
    │ LB :80     │  │ LB :8081   │  │   :8080     │
    └─────┬──────┘  └─────┬──────┘  └─────────────┘
          │               │
    ┌─────┼─────┐   ┌─────┼─────┐
    │     │     │   │     │     │
  ┌─▼─┐┌─▼─┐┌─▼─┐┌─▼─┐┌─▼─┐┌─▼─┐   ┌────────────┐
  │ F1 ││ F2 ││ F3 ││ B1 ││ B2 ││ B3 │──▶│ PostgreSQL │
  └────┘└────┘└────┘└────┘└────┘└────┘   │  :5432     │
                                          └────────────┘
```

- **3 instancias Frontend** (React + Nginx) con load balancer
- **3 instancias Backend** (Spring Boot) con load balancer
- **Keycloak** para autenticación OAuth2/OpenID Connect
- **PostgreSQL** como base de datos

