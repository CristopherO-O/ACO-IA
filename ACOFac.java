import java.util.List;

public class ACOFac {

    public static void main(String[] args) {

        // ------------------------------------------------------------
        // DADOS DO PROBLEMA (tempos de processamento das 30 tarefas)
        // ------------------------------------------------------------
        int[] processingTime = {
                12, 5, 9, 7, 4, 11, 8, 6, 10, 3,
                7, 9, 5, 6, 4, 8, 12, 3, 7, 8,
                3, 7, 8, 11, 4, 9, 10, 13, 5, 2
        };

        int numMachines = 5;

        // ------------------------------------------------------------
        // PARÂMETROS DO ACO
        // ------------------------------------------------------------
        int numAnts = 30;          // uma formiga por tarefa
        int numIterations = 300;
        double alpha = 1.0;        // peso do feromônio
        double beta = 2.0;         // peso da heurística (carga da máquina)
        double rho = 0.1;          // taxa de evaporação
        double Q = 100.0;          // constante de depósito de feromônio
        double initialPheromone = 1.0;

        AntColonyPMS colony = new AntColonyPMS(
                processingTime, numMachines, numAnts, numIterations,
                alpha, beta, rho, Q, initialPheromone
        );

        List<Integer> assignment = colony.solveInstant();

        printReport(processingTime, numMachines, assignment, colony);
    }

    private static void printReport(int[] processingTime, int numMachines,
                                     List<Integer> assignment, AntColonyPMS colony) {

        System.out.println("\n===============================================");
        System.out.println("RELATÓRIO FINAL - ACO PARA MÁQUINAS PARALELAS");
        System.out.println("===============================================\n");

        // a) Atribuição final de tarefas às máquinas
        System.out.println("a) Atribuição final (Tarefa -> Máquina):");
        double[] machineLoad = new double[numMachines + 1];
        for (int task = 0; task < assignment.size(); task++) {
            int machine = assignment.get(task);
            machineLoad[machine] += processingTime[task];
        }

        for (int m = 1; m <= numMachines; m++) {
            StringBuilder sb = new StringBuilder("  Máquina " + m + ": [");
            boolean first = true;
            for (int task = 0; task < assignment.size(); task++) {
                if (assignment.get(task) == m) {
                    if (!first) sb.append(", ");
                    sb.append("T").append(task + 1).append("(").append(processingTime[task]).append(")");
                    first = false;
                }
            }
            sb.append("]  -> carga = ").append((int) machineLoad[m]);
            System.out.println(sb);
        }

        // b) Valor final do makespan
        System.out.printf("%nb) Makespan final (C_max) = %.2f%n", colony.getBestMakespan());

        // c) Tempo de execução
        System.out.printf("%nc) Tempo de execução = %.3f ms%n", colony.getElapsedNanos() / 1_000_000.0);

        // d) Evolução da solução (somente quando o melhor makespan melhora)
        System.out.println("\nd) Melhoras do makespan por iteração:");
        List<Double> history = colony.getBestMakespanHistory();
        for (int i = 0; i < history.size(); i++) {
            if (i == 0 || history.get(i) < history.get(i - 1)) {
                System.out.printf("  Iteração %3d -> Makespan = %.2f%n", i + 1, history.get(i));
            }
        }

        // salva histórico em CSV para plotar o gráfico de convergência
        saveHistoryCsv(history);
    }

    private static void saveHistoryCsv(List<Double> history) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter("convergencia_facil.csv")) {
            writer.println("iteracao,melhor_makespan");
            for (int i = 0; i < history.size(); i++) {
                writer.printf("%d,%.4f%n", i + 1, history.get(i));
            }
            System.out.println("\n[OK] Histórico de convergência salvo em convergencia_facil.csv (para plotar o gráfico)");
        } catch (Exception e) {
            System.out.println("Não foi possível salvar o CSV de convergência: " + e.getMessage());
        }
    }
}