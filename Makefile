.PHONY: run stop install build backend frontend-admin frontend-user

# ─── Start everything ────────────────────────────────────────────
run:
	@bash scripts/run.sh

# ─── Individual services ─────────────────────────────────────────
backend:
	mvn compile exec:java -Dexec.mainClass="com.iiit.oms.OmsApplication"

frontend-admin:
	cd frontend && npm run dev

frontend-user:
	cd user-frontend && npm run dev

# ─── Setup ───────────────────────────────────────────────────────
install:
	@echo "Installing frontend dependencies..."
	@cd frontend && npm install
	@cd user-frontend && npm install
	@echo "Done."

# ─── Build ───────────────────────────────────────────────────────
build:
	@echo "Building backend..."
	@mvn -q package -DskipTests
	@echo "Building admin frontend..."
	@cd frontend && npm run build
	@echo "Building user frontend..."
	@cd user-frontend && npm run build
	@echo "All builds complete."

# ─── Stop background processes ───────────────────────────────────
stop:
	@pkill -f "exec:java.*OmsApplication" 2>/dev/null || true
	@pkill -f "vite" 2>/dev/null || true
	@echo "Services stopped."
