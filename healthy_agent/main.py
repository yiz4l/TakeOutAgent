"""Run the standalone Python health-analysis worker."""
from agent.config import Settings
from agent.worker import HealthAnalysisWorker

def main() -> None:
    HealthAnalysisWorker(Settings.from_env()).run_forever()

if __name__ == "__main__":
    main()

