# Oracle Cloud Free Tier Deployment (Tickify)

This guide deploys Tickify on an Oracle Cloud Always Free VM (Ubuntu) using Payara Micro + systemd + NGINX.

You can now run full automation from your local machine using your OCI profile:

- Provision VM in your Oracle tenancy
- Copy project to VM
- Configure environment
- Build and deploy Tickify

Script:

- `deploy/oracle-free-tier/provision-and-deploy-oci.sh`

## 1. One-command automated provisioning + deploy

Prerequisites on your local machine:

1. OCI CLI is installed and authenticated (`oci setup config`).
2. You have:
- `COMPARTMENT_ID`
- `SUBNET_ID`
- SSH key pair
3. OCI network allows inbound `22` and `80`.

Run:

```bash
cd Tickify-SWP-Web-App_Copy
chmod +x deploy/oracle-free-tier/*.sh

OCI_PROFILE=DEFAULT \
COMPARTMENT_ID="ocid1.compartment..." \
SUBNET_ID="ocid1.subnet..." \
SSH_PUBLIC_KEY_PATH="$HOME/.ssh/id_rsa.pub" \
SSH_PRIVATE_KEY_PATH="$HOME/.ssh/id_rsa" \
TICKIFY_DB_USER="your_db_user" \
TICKIFY_DB_PASSWORD="your_db_password" \
TICKIFY_DB_HOST="127.0.0.1" \
TICKIFY_DB_PORT="1527" \
TICKIFY_DB_NAME="tickifyDB" \
./deploy/oracle-free-tier/provision-and-deploy-oci.sh
```

Optional variables:

- `VM_DISPLAY_NAME` (default auto-generated)
- `SHAPE` (default `VM.Standard.E2.1.Micro`)
- `IMAGE_ID` (if you want to pin an image)
- `APP_BASE_URL` (defaults to VM public IP)
- SMTP variables (`TICKIFY_SMTP_*`)

## 2. Manual flow (if you prefer)

1. Create an Ubuntu VM in Oracle Cloud (Ampere A1 or AMD Always Free).
2. Open inbound port 80 in both:
- OCI VCN Security List / Network Security Group
- VM firewall (`ufw`) if enabled

## 3. Copy project to VM

Example:

```bash
scp -i /path/to/key -r Tickify-SWP-Web-App_Copy ubuntu@<VM_PUBLIC_IP>:/home/ubuntu/
```

## 4. Run setup script on VM

```bash
ssh -i /path/to/key ubuntu@<VM_PUBLIC_IP>
cd /home/ubuntu/Tickify-SWP-Web-App_Copy
chmod +x deploy/oracle-free-tier/setup-vm.sh deploy/oracle-free-tier/deploy-app.sh
./deploy/oracle-free-tier/setup-vm.sh
```

## 5. Configure environment values

```bash
sudo cp deploy/oracle-free-tier/tickify.env.example /opt/tickify/config/tickify.env
sudo nano /opt/tickify/config/tickify.env
```

Set at minimum:
- `TICKIFY_DB_USER`
- `TICKIFY_DB_PASSWORD`
- `TICKIFY_DB_HOST`
- `TICKIFY_DB_PORT`
- `TICKIFY_DB_NAME`
- `JAVA_OPTS` with your VM public IP in `tickify.app.baseUrl`

## 6. Deploy application

```bash
cd /home/ubuntu/Tickify-SWP-Web-App_Copy
./deploy/oracle-free-tier/deploy-app.sh
```

The app will be exposed through NGINX:

- `http://<VM_PUBLIC_IP>/`

## 7. Verify service health

```bash
sudo systemctl status tickify --no-pager
sudo systemctl status nginx --no-pager
sudo journalctl -u tickify -n 100 --no-pager
```

## 8. Update / redeploy after code changes

```bash
cd /home/ubuntu/Tickify-SWP-Web-App_Copy
./deploy/oracle-free-tier/deploy-app.sh
```

## Notes

1. The deploy script patches `nbproject/project.properties` inside the VM copy so NetBeans local absolute jar references become VM-local references.
2. Payara Micro runs Tickify on `127.0.0.1:8080` and NGINX reverse proxies traffic on port 80.
3. If you want HTTPS, add Certbot and configure TLS on NGINX.

## Terminate VM automation

```bash
OCI_PROFILE=DEFAULT \
INSTANCE_ID="ocid1.instance..." \
./deploy/oracle-free-tier/terminate-oci-vm.sh
```
