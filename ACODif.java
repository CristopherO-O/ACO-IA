import java.util.ArrayList;
import java.util.List;

public class ACODif {

	public static void main(String[] args) {

		// ------------------------------------------------------------
		// DADOS DO PROBLEMA (40 tarefas, tempos de processamento)
		// índice 0 = Tarefa 1, índice 39 = Tarefa 40
		// ------------------------------------------------------------
		int[] processingTime = {
				25, 17, 20, 12, 28, 16, 22, 15, 18, 30, // T1..T10
				19, 23, 11, 27, 14, 21, 17, 24, 26, 19, // T11..T20
				13, 10, 15, 28, 22, 18, 21, 30, 23, 17, // T21..T30
				25, 20, 22, 16, 18, 12, 26, 14, 27, 11  // T31..T40
		};

		int numTasks = processingTime.length; 
		int numMachines = 5;                  

		// ------------------------------------------------------------
		// RESTRIÇÕES DE PRECEDÊNCIA
		// successors[i] = tarefas que só podem começar depois que a
		// tarefa i terminar (tudo 0-indexado; Tarefa k -> índice k-1)
		// ------------------------------------------------------------
		List<Integer>[] successors = new List[numTasks];
		for (int i = 0; i < numTasks; i++) successors[i] = new ArrayList<>();

		addPrecedence(successors, 1, 2, 3);
		addPrecedence(successors, 3, 4, 5);
		addPrecedence(successors, 5, 6);
		addPrecedence(successors, 8, 9);
		addPrecedence(successors, 10, 11);
		addPrecedence(successors, 12, 13);
		addPrecedence(successors, 15, 16);
		addPrecedence(successors, 17, 18);
		addPrecedence(successors, 19, 20);
		addPrecedence(successors, 21, 22);
		addPrecedence(successors, 23, 24);
		addPrecedence(successors, 25, 26);
		addPrecedence(successors, 27, 28);
		addPrecedence(successors, 29, 30);
		addPrecedence(successors, 31, 32);
		addPrecedence(successors, 33, 34);
		addPrecedence(successors, 35, 36);
		addPrecedence(successors, 37, 38);
		addPrecedence(successors, 39, 40);

		int numAnts = 40;
		int numIterations = 300;
		double alpha = 1.0;
		double beta = 2.0;
		double rho = 0.1;
		double Q = 100.0;
		double initialPheromone = 1.0;

		AntColonyDif colony = new AntColonyDif(
				processingTime, numMachines, successors, numAnts, numIterations,
				alpha, beta, rho, Q, initialPheromone
		);

		int[] assignment = colony.solveInstant();

		printReport(processingTime, numMachines, assignment, colony);
	}

	private static void addPrecedence(List<Integer>[] successors, int from, int... to) {
		for (int t : to) {
			successors[from - 1].add(t - 1);
		}
	}

	private static void printReport(int[] processingTime, int numMachines,
									 int[] assignment, AntColonyDif colony) {

		int numTasks = processingTime.length;
		double[] start = colony.getBestStart();
		double[] finish = colony.getBestFinish();

		System.out.println("\n===============================================");
		System.out.println("RELATÓRIO FINAL - ACO PARA MÁQUINAS PARALELAS IDÊNTICAS");
		System.out.println("COM RESTRIÇÕES DE PRECEDÊNCIA E NÃO PREEMPÇÃO");
		System.out.println("===============================================\n");

		System.out.println("a) Atribuição final (Tarefa -> Máquina), com início/fim de execução:");
		double[] machineLoad = colony.calculateMachineLoads(assignment);

		for (int m = 1; m <= numMachines; m++) {
			System.out.println("  Máquina " + m + ":");
			List<Integer> tasksOnMachine = new ArrayList<>();
			for (int task = 0; task < numTasks; task++) {
				if (assignment[task] == m) tasksOnMachine.add(task);
			}
			tasksOnMachine.sort((a, b) -> Double.compare(start[a], start[b]));
			for (int task : tasksOnMachine) {
				System.out.printf("    T%-2d (tempo=%2d)  início=%6.2f  fim=%6.2f%n",
						task + 1, processingTime[task], start[task], finish[task]);
			}
			System.out.printf("    -> tempo total ocupado nessa máquina = %.2f%n", machineLoad[m - 1]);
		}

		System.out.printf("%nb) Makespan final (C_max) = %.2f%n", colony.getBestMakespan());

		boolean ok = colony.validatePrecedence(start, finish);
		System.out.println("   Restrições de precedência respeitadas: " + (ok ? "SIM" : "NÃO"));

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
		try (java.io.PrintWriter writer = new java.io.PrintWriter("convergencia_dif.csv")) {
			writer.println("iteracao,melhor_makespan");
			for (int i = 0; i < history.size(); i++) {
				writer.printf("%d,%.4f%n", i + 1, history.get(i));
			}
			System.out.println("\n[OK] Histórico de convergência salvo em convergencia_dif.csv (para plotar o gráfico)");
		} catch (Exception e) {
			System.out.println("Não foi possível salvar o CSV de convergência: " + e.getMessage());
		}
	}
}