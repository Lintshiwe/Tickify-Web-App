# Tickify AWS Multi-Tier Architecture (Current Deployment)

## 1. Tier Model

1. Presentation tier
- JSP views under `web/` (role dashboards, checkout, profile, admin, scanner views)
- Browser clients (Attendee, Admin, EventManager, Presenter, VenueGuard)

2. Application tier
- Java EE web app (WAR) on Payara Micro
- HTTP listener: `:8080`
- Controller layer: servlet classes in `src/java/za/ac/tut/servlet`
- Business service layer: `src/java/za/ac/tut/application/*`

3. Data tier
- DAO and DB integration packages:
  - `src/java/za/ac/tut/databaseManagement`
  - `src/java/za/ac/tut/databaseConnection`
- Entities in `src/java/za/ac/tut/entities`

## 2. AWS Services In Use

1. Amazon EC2
- Ubuntu VM hosting app runtime
- systemd service: `tickify`

2. Amazon VPC networking
- Security Group must allow:
  - `22/tcp` (SSH)
  - `8080/tcp` (direct app mode)
  - `80/tcp` only if reverse proxy mode is enabled

3. Optional reverse proxy
- nginx on EC2 (optional, can be disabled)
- For constrained VMs, direct app mode on `:8080` is recommended

## 3. Runtime/Auto-start Design

1. App process management
- systemd unit: `deploy/oracle-free-tier/tickify.service`
- `Restart=always` and boot auto-start via `WantedBy=multi-user.target`

2. VM bootstrap and deploy automation
- base setup + dependency provisioning: `deploy/oracle-free-tier/setup-vm.sh`
- build + deploy + optional proxy: `deploy/oracle-free-tier/deploy-app.sh`
- EC2 wrapper: `deploy/aws-ec2/deploy-to-ec2.sh`

## 4. API Wiring and Communication Evidence

1. Servlet endpoint registry
- `web/WEB-INF/web.xml` defines servlet classes and `*.do` URL mappings
- Includes login, registration, dashboard, tickets, checkout/payment, password reset, admin/guard endpoints

2. Controller inventory
- Workspace includes dozens of servlet controllers in `src/java/za/ac/tut/servlet`
- Controllers extend `HttpServlet` and route request/response handling

3. Layered communication
- `docs/THREE_TIER_ARCHITECTURE.md` documents controller -> application service -> DAO flow

## 5. Reliability Checks (Operational)

Run on VM:

```bash
sudo systemctl is-enabled tickify
sudo systemctl is-active tickify
sudo journalctl -u tickify -n 200 --no-pager
sudo ss -lntp | grep 8080
curl -I http://127.0.0.1:8080/Tickify-SWP-Web-App/
```

Pass criteria:
1. `is-enabled` = `enabled`
2. `is-active` = `active`
3. HTTP status is not `5xx`
4. No recurring OOM/restart loop in journal

## 6. Scalability Checks (Practical Baseline)

Single-VM checks:

```bash
# CPU and memory pressure while app is running
uptime
free -m
vmstat 1 5

# simple smoke concurrency
for i in $(seq 1 20); do curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/Tickify-SWP-Web-App/ & done; wait
```

Recommended next scaling step:
1. Move to ALB + Auto Scaling Group with immutable EC2 instances.
2. Externalize DB to managed database and session strategy.
3. Keep app stateless and bake image (AMI) for horizontal scaling.

## 7. Security/Resilience Notes

1. Set conservative JVM heap on small instances (example: `-Xms128m -Xmx384m`).
2. Keep `tickify` service restart policy enabled.
3. Use CloudWatch logs/metrics and alarms for memory, CPU, restart count, and 5xx rate.
4. Restrict inbound ports strictly to required traffic.

## 8. Suggested Deployment Modes

1. Direct mode (recommended for now)
- `NGINX_ENABLED=false`
- Public URL: `http://<ec2-host>:8080/Tickify-SWP-Web-App/`

2. Reverse proxy mode (optional)
- `NGINX_ENABLED=true`
- Public URL: `http://<ec2-host>/`
