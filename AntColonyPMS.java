package src.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AntColonyPMS {
	private final int[] processingTime;
	private final int numMachines;
	private final int numAnts;
	private final int numIterations;
	private final double alpha;
	private final double beta;
	private final double rho;
	private final double q;
	private final double[][] pheromone;
	private final Random random = new Random();
	private final List<Double> bestMakespanHistory = new ArrayList<>();
	private List<Integer> bestAssignment;
	private double bestMakespan = Double.POSITIVE_INFINITY;
	private long elapsedNanos;

	public AntColonyPMS(int[] processingTime, int numMachines, int numAnts,
						int numIterations, double alpha, double beta,
						double rho, double q, double initialPheromone) {
		this.processingTime = processingTime.clone();
		this.numMachines = numMachines;
		this.numAnts = numAnts;
		this.numIterations = numIterations;
		this.alpha = alpha;
		this.beta = beta;
		this.rho = rho;
		this.q = q;
		this.pheromone = new double[processingTime.length][numMachines];
		for (int task = 0; task < processingTime.length; task++) {
			for (int machine = 0; machine < numMachines; machine++) {
				pheromone[task][machine] = initialPheromone;
			}
		}
	}

	public List<Integer> solveInstant() {
		long start = System.nanoTime();
		bestAssignment = null;
		bestMakespan = Double.POSITIVE_INFINITY;
		bestMakespanHistory.clear();

		for (int iteration = 0; iteration < numIterations; iteration++) {
			List<List<Integer>> assignments = new ArrayList<>();
			List<Double> makespans = new ArrayList<>();
			for (int ant = 0; ant < numAnts; ant++) {
				List<Integer> assignment = buildAssignment();
				double makespan = calculateMakespan(assignment);
				assignments.add(assignment);
				makespans.add(makespan);
				if (makespan < bestMakespan) {
					bestMakespan = makespan;
					bestAssignment = new ArrayList<>(assignment);
				}
			}

			for (int task = 0; task < processingTime.length; task++) {
				for (int machine = 0; machine < numMachines; machine++) {
					pheromone[task][machine] *= 1.0 - rho;
				}
			}
			for (int ant = 0; ant < assignments.size(); ant++) {
				double deposit = q / Math.max(makespans.get(ant), 1.0);
				List<Integer> assignment = assignments.get(ant);
				for (int task = 0; task < assignment.size(); task++) {
					pheromone[task][assignment.get(task) - 1] += deposit;
				}
			}
			bestMakespanHistory.add(bestMakespan);
		}
		elapsedNanos = System.nanoTime() - start;
		return new ArrayList<>(bestAssignment);
	}

	private List<Integer> buildAssignment() {
		List<Integer> assignment = new ArrayList<>(processingTime.length);
		double[] loads = new double[numMachines];
		for (int task = 0; task < processingTime.length; task++) {
			double[] probability = new double[numMachines];
			double total = 0;
			for (int machine = 0; machine < numMachines; machine++) {
				double heuristic = 1.0 / (loads[machine] + processingTime[task]);
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
			assignment.add(selected + 1);
			loads[selected] += processingTime[task];
		}
		return assignment;
	}

	private double calculateMakespan(List<Integer> assignment) {
		double[] loads = new double[numMachines];
		for (int task = 0; task < assignment.size(); task++) {
			loads[assignment.get(task) - 1] += processingTime[task];
		}
		double result = 0;
		for (double load : loads) {
			result = Math.max(result, load);
		}
		return result;
	}

	public double getBestMakespan() {
		return bestMakespan;
	}

	public long getElapsedNanos() {
		return elapsedNanos;
	}

	public List<Double> getBestMakespanHistory() {
		return new ArrayList<>(bestMakespanHistory);
	}
}
