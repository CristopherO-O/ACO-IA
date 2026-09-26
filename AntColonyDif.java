import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AntColonyDif {

	private final int[] processingTime;  
	private final int numTasks;
	private final int numMachines;

	private final List<Integer>[] predecessors;

	private final int[] topoOrder;

	private final int numAnts;
	private final int numIterations;
	private final double alpha;
	private final double beta;
	private final double rho;
	private final double q;

	private final double[][] pheromone; // [tarefa][máquina]
	private final Random random = new Random();

	private final List<Double> bestMakespanHistory = new ArrayList<>();
	private int[] bestAssignment;       
	private double[] bestStart;
	private double[] bestFinish;
	private double bestMakespan = Double.POSITIVE_INFINITY;
	private long elapsedNanos;

	public AntColonyDif(int[] processingTime, int numMachines, List<Integer>[] successors,
								int numAnts, int numIterations, double alpha, double beta,
								double rho, double q, double initialPheromone) {
		this.processingTime = processingTime.clone();
		this.numTasks = processingTime.length;
		this.numMachines = numMachines;
		this.numAnts = numAnts;
		this.numIterations = numIterations;
		this.alpha = alpha;
		this.beta = beta;
		this.rho = rho;
		this.q = q;

		// inverte successors -> predecessors
		this.predecessors = new List[numTasks];
		for (int t = 0; t < numTasks; t++) {
			this.predecessors[t] = new ArrayList<>();
		}
		if (successors != null) {
			for (int t = 0; t < numTasks; t++) {
				if (successors[t] == null) continue;
				for (int succ : successors[t]) {
					this.predecessors[succ].add(t);
				}
			}
		}

		this.topoOrder = computeTopologicalOrder(successors);

		this.pheromone = new double[numTasks][numMachines];
		for (int task = 0; task < numTasks; task++) {
			for (int machine = 0; machine < numMachines; machine++) {
				pheromone[task][machine] = initialPheromone;
			}
		}
	}

	private int[] computeTopologicalOrder(List<Integer>[] successors) {
		int[] inDegree = new int[numTasks];
		if (successors != null) {
			for (int t = 0; t < numTasks; t++) {
				if (successors[t] == null) continue;
				for (int succ : successors[t]) {
					inDegree[succ]++;
				}
			}
		}
		List<Integer> queue = new ArrayList<>();
		for (int t = 0; t < numTasks; t++) {
			if (inDegree[t] == 0) queue.add(t);
		}
		int[] order = new int[numTasks];
		int idx = 0;
		int head = 0;
		List<Integer> workQueue = new ArrayList<>(queue);
		while (head < workQueue.size()) {
			int t = workQueue.get(head++);
			order[idx++] = t;
			if (successors == null || successors[t] == null) continue;
			for (int succ : successors[t]) {
				inDegree[succ]--;
				if (inDegree[succ] == 0) {
					workQueue.add(succ);
				}
			}
		}
		if (idx != numTasks) {
			throw new IllegalStateException("Grafo de precedência possui ciclo — não é possível escalonar.");
		}
		return order;
	}

	public int[] solveInstant() {
		long start = System.nanoTime();
		bestAssignment = null;
		bestMakespan = Double.POSITIVE_INFINITY;
		bestMakespanHistory.clear();

		for (int iteration = 0; iteration < numIterations; iteration++) {
			List<int[]> assignments = new ArrayList<>();
			List<Double> makespans = new ArrayList<>();

			for (int ant = 0; ant < numAnts; ant++) {
				Schedule schedule = buildAssignment();
				assignments.add(schedule.assignment);
				makespans.add(schedule.makespan);
				if (schedule.makespan < bestMakespan) {
					bestMakespan = schedule.makespan;
					bestAssignment = schedule.assignment.clone();
					bestStart = schedule.start.clone();
					bestFinish = schedule.finish.clone();
				}
			}

			// evaporação
			for (int task = 0; task < numTasks; task++) {
				for (int machine = 0; machine < numMachines; machine++) {
					pheromone[task][machine] *= 1.0 - rho;
				}
			}
			// depósito
			for (int ant = 0; ant < assignments.size(); ant++) {
				double deposit = q / Math.max(makespans.get(ant), 1.0);
				int[] assignment = assignments.get(ant);
				for (int task = 0; task < numTasks; task++) {
					pheromone[task][assignment[task] - 1] += deposit;
				}
			}
			bestMakespanHistory.add(bestMakespan);
		}
		elapsedNanos = System.nanoTime() - start;
		return bestAssignment.clone();
	}

	private static final class Schedule {
		final int[] assignment;
		final double[] start;
		final double[] finish;
		final double makespan;

		Schedule(int[] assignment, double[] start, double[] finish, double makespan) {
			this.assignment = assignment;
			this.start = start;
			this.finish = finish;
			this.makespan = makespan;
		}
	}

	private Schedule buildAssignment() {
		int[] assignment = new int[numTasks];
		double[] machineFree = new double[numMachines];
		double[] start = new double[numTasks];
		double[] finish = new double[numTasks];

		for (int task : topoOrder) {
			double predFinish = 0.0;
			for (int pred : predecessors[task]) {
				predFinish = Math.max(predFinish, finish[pred]);
			}

			double[] probability = new double[numMachines];
			double total = 0;
			for (int machine = 0; machine < numMachines; machine++) {
				double earliestStart = Math.max(machineFree[machine], predFinish);
				double completionIfHere = earliestStart + processingTime[task];
				double heuristic = 1.0 / completionIfHere;
				probability[machine] = Math.pow(pheromone[task][machine], alpha)
						* Math.pow(heuristic, beta);
				total += probability[machine];
			}

			double choice = random.nextDouble() * total;
			int selected = numMachines - 1;
			for (int machine = 0; machine < numMachines; machine++) {
				choice -= probability[machine];
				if (choice <= 0) {
					selected = machine;
					break;
				}
			}

			double taskStart = Math.max(machineFree[selected], predFinish);
			double taskFinish = taskStart + processingTime[task];

			assignment[task] = selected + 1;
			start[task] = taskStart;
			finish[task] = taskFinish;
			machineFree[selected] = taskFinish;
		}

		double makespan = 0;
		for (double f : finish) {
			makespan = Math.max(makespan, f);
		}
		return new Schedule(assignment, start, finish, makespan);
	}

	public double[][] recomputeSchedule(int[] assignment) {
		double[] machineFree = new double[numMachines];
		double[] start = new double[numTasks];
		double[] finish = new double[numTasks];
		for (int task : topoOrder) {
			double predFinish = 0.0;
			for (int pred : predecessors[task]) {
				predFinish = Math.max(predFinish, finish[pred]);
			}
			int machine = assignment[task] - 1;
			double taskStart = Math.max(machineFree[machine], predFinish);
			double taskFinish = taskStart + processingTime[task];
			start[task] = taskStart;
			finish[task] = taskFinish;
			machineFree[machine] = taskFinish;
		}
		return new double[][] { start, finish };
	}

	public boolean validatePrecedence(double[] start, double[] finish) {
		for (int task = 0; task < numTasks; task++) {
			for (int pred : predecessors[task]) {
				if (start[task] < finish[pred] - 1e-9) {
					return false;
				}
			}
		}
		return true;
	}

	public double[] calculateMachineLoads(int[] assignment) {
		double[] loads = new double[numMachines];
		for (int task = 0; task < numTasks; task++) {
			loads[assignment[task] - 1] += processingTime[task];
		}
		return loads;
	}

	public double getBestMakespan() {
		return bestMakespan;
	}

	public double[] getBestStart() {
		return bestStart.clone();
	}

	public double[] getBestFinish() {
		return bestFinish.clone();
	}

	public long getElapsedNanos() {
		return elapsedNanos;
	}

	public List<Double> getBestMakespanHistory() {
		return new ArrayList<>(bestMakespanHistory);
	}

	public int[] getTopoOrder() {
		return topoOrder.clone();
	}
}