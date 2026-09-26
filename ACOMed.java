
import java.util.List;

public class ACOMed {

    public static void main(String[] args) {

        int[] processingTime = {
                17, 9, 20, 12, 28, 16, 22, 15, 18, 22,
                19, 23, 11, 27, 14, 21, 17, 24, 26, 15,
                13, 10, 15, 16, 22, 18, 21, 20, 23, 17,
                23, 20, 22, 16, 18, 12, 26, 14, 20, 11,
                20, 18, 23, 15, 17, 8, 21, 14, 23, 12
        };

        double[] machineCapacity = { 18, 22, 25, 16, 28, 14 };

        int numAnts = 50;          // uma formiga por tarefa
        int numIterations = 300;
        double alpha = 1.0;        // peso do feromônio
        double beta = 2.0;         // peso da heurística (carga/velocidade da máquina)
        double rho = 0.1;          // taxa de evaporação
        double Q = 100.0;          // constante de depósito de feromônio
        double initialPheromone = 1.0;

        AntColonyMed colony = new AntColonyMed(
                processingTime, machineCapacity, numAnts, numIterations,
                alpha, beta, rho, Q, initialPheromone
        );

        List<Integer> assignment = colony.solveInstant();

        printReport(processingTime, machineCapacity, assignment, colony);
    }

    private static void printReport(int[] processingTime, double[] machineCapacity,
                                     List<Integer> assignment, AntColonyMed colony) {

        int numMachines = machineCapacity.length;

        System.out.println("\n===============================================");
        System.out.println("RELATÓRIO FINAL - ACO PARA MÁQUINAS PARALELAS NÃO IDÊNTICAS");
        System.out.println("===============================================\n");

        System.out.println("a) Atribuição final (Tarefa -> Máquina):");
        double[] machineLoad = colony.calculateMachineLoads(assignment);

        for (int m = 1; m <= numMachines; m++) {
            StringBuilder sb = new StringBuilder("  Máquina " + m
                    + " (capacidade=" + (int) machineCapacity[m - 1] + "): [");
            boolean first = true;
            for (int task = 0; task < assignment.size(); task++) {
                if (assignment.get(task) == m) {
                    if (!first) sb.append(", ");
                    sb.append("T").append(task + 1).append("(base=").append(processingTime[task]).append(")");
                    first = false;
                }
            }
            sb.append("]  -> tempo total nessa máquina = ")
              .append(String.format("%.2f", machineLoad[m - 1]));
            System.out.println(sb);
        }

        System.out.printf("%nb) Makespan final (C_max) = %.2f%n", colony.getBestMakespan());

        System.out.printf("%nc) Tempo de execução = %.3f ms%n", colony.getElapsedNanos() / 1_000_000.0);

        System.out.println("\nd) Melhoras do makespan por iteração:");
        List<Double> history = colony.getBestMakespanHistory();
        for (int i = 0; i < history.size(); i++) {
            if (i == 0 || history.get(i) < history.get(i - 1)) {
                System.out.printf("  Iteração %3d -> Makespan = %.2f%n", i + 1, history.get(i));
            }
        }

        saveHistoryCsv(history);
    }

    private static void saveHistoryCsv(List<Double> history) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter("convergencia_med.csv")) {
            writer.println("iteracao,melhor_makespan");
            for (int i = 0; i < history.size(); i++) {
                writer.printf("%d,%.4f%n", i + 1, history.get(i));
            }
            System.out.println("\n[OK] Histórico de convergência salvo em convergencia_med.csv (para plotar o gráfico)");
        } catch (Exception e) {
            System.out.println("Não foi possível salvar o CSV de convergência: " + e.getMessage());
        }
    }
}