package common;

import java.util.Random;

public class MatrixOperations {
	
	public static int[] initializeIndices(int size) {
		int[] indices = new int[size];
		for (int i = 0; i < indices.length; ++i) {
			indices[i] = i;
		}
		return indices;
	}
	
	public static void shuffle(int[] vector) {
		Random random = new Random();
		//start from last element and work your way to the front
		for (int i = vector.length-1; i > 0; --i) {
			//pick a random index smaller than the current one
			int index = random.nextInt(i);
			//swap elements in those positions
			int element = vector[i];
			vector[i] = vector[index];
			vector[index] = element;
		}
	}
	
	public static double[][] transpose(double[][] matrix) {
		double[][] result = new double[matrix[0].length][matrix.length];
		for (int row = 0; row < matrix.length; ++row) {
			for (int col = 0; col < matrix[0].length; ++col) {
				result[col][row] = matrix[row][col];
			}
		}
		return result;
	}
	
	public static double[] randomInitialize(int size, double lowerLimit, double upperLimit) {
		Random random = new Random();
		double[] result = new double[size];
		for (int i = 0; i < result.length; ++i) {
			result[i] = MatrixOperations.scale(random.nextDouble(), 0, 1, lowerLimit, upperLimit);
		}
		return result;
	}
	
	public static double[][] randomInitialize(int rows, int cols, double lowerLimit, double upperLimit) {
		Random random = new Random();
		double[][] result = new double[rows][cols];
		for (int row = 0; row < result.length; ++row) {
			for (int col = 0; col < result[0].length; ++col) {
				result[row][col] = MatrixOperations.scale(random.nextDouble(), 0, 1, lowerLimit, upperLimit);
			}
		}
		return result;
	}
	
	public static double[] multiply(double[][] matrix, double[] vector) {
		double[] result = new double[matrix.length];
		for (int i = 0; i < matrix.length; ++i) {
			result[i] = MatrixOperations.dotProduct(matrix[i], vector);
		}
		return result;
	}
	
	public static double[][] multiply(double[][] matrix, double number) {
		double[][] result = new double[matrix.length][matrix[0].length];
		for (int row = 0; row < result.length; ++row) {
			for (int col = 0; col < result[0].length; ++col) {
				result[row][col] = matrix[row][col] * number;
			}
		}
		return result;
	}
	
	public static double[] multiply(double[] vector, double number) {
		double[] result = new double[vector.length];
		for (int i = 0; i < result.length; ++i) {
			result[i] = vector[i] * number;
		}
		return result;
	}
	
	public static void addInline(double[] vector1, double[] vector2) {
		if (vector1.length != vector2.length) {
			throw new RuntimeException("MatrixOperations::addInline: vectors have different sizes");
		}
		for (int i = 0; i < vector1.length; ++i) {
			vector1[i] += vector2[i];
		}
	}
	
	public static void addInline(double[][] matrix1, double[][] matrix2) {
		if (matrix1.length != matrix2.length || matrix1[0].length != matrix2[0].length) {
			throw new RuntimeException("MatrixOperations::addInline: matrices have different sizes");
		}
		for (int row = 0; row < matrix1.length; ++row) {
			for (int col = 0; col < matrix1[0].length; ++col) {
				matrix1[row][col] += matrix2[row][col];
			}
		}
	}
	
	public static void subtractInline(double[] vector1, double[] vector2) {
		if (vector1.length != vector2.length) {
			throw new RuntimeException("MatrixOperations::subtractInline: vectors have different sizes");
		}
		for (int i = 0; i < vector1.length; ++i) {
			vector1[i] -= vector2[i];
		}
	}
	
	public static void subtractInline(double[][] matrix1, double[][] matrix2) {
		if (matrix1.length != matrix2.length || matrix1[0].length != matrix2[0].length) {
			throw new RuntimeException("MatrixOperations::subtractInline: matrices have different sizes");
		}
		for (int row = 0; row < matrix1.length; ++row) {
			for (int col = 0; col < matrix1[0].length; ++col) {
				matrix1[row][col] -= matrix2[row][col];
			}
		}
	}
	
	public static void multiplyInline(double[] vector, double number) {
		for (int i = 0; i < vector.length; ++i) {
			vector[i] *= number;
		}
	}
	
	public static void multiplyInline(double[][] matrix, double number) {
		for (int row = 0; row < matrix.length; ++row) {
			for (int col = 0; col < matrix[0].length; ++col) {
				matrix[row][col]*= number;
			}
		}
	}
	
	public static double dotProduct(double[] vector1, double[] vector2) {
		if (vector1.length != vector2.length) {
			throw new RuntimeException("MatrixOperations::dotProduct: vectors have different sizes");
		}
		double result = 0;
		for (int i = 0; i < vector1.length; ++i) {
			result += vector1[i] * vector2[i];
		}
		return result;
	}
	
	public static double[][] outerProduct(double[] vector1, double[] vector2) {
		double[][] matrix = new double[vector1.length][vector2.length];
		for (int row = 0; row < vector1.length; ++row) {
			for (int col = 0; col < vector2.length; ++col) {
				matrix[row][col] = vector1[row] * vector2[col];
			}
		}
		return matrix;
	}
	
	public static double[] multiplyComponentWise(double[] vector1, double[] vector2) {
		if (vector1.length != vector2.length) {
			throw new RuntimeException("MatrixOperations::multiplyComponentWise: vectors have different sizes");
		}
		double[] result = new double[vector1.length];
		for (int i = 0; i < vector1.length; ++i) {
			result[i] = vector1[i] * vector2[i];
		}
		return result;
	}
	
	public static double[] sum(double[] vector1, double[] vector2) {
		if (vector1.length != vector2.length) {
			throw new RuntimeException("MatrixOperations::sum: vectors have different sizes");
		}
		double[] result = new double[vector1.length];
		for (int i = 0; i < result.length; ++i) {
			result[i] = vector1[i] + vector2[i];
		}
		return result;
	}

	public static double[] powComponentWise(double[] vector, double power) {
		double[] result = new double[vector.length];
		for (int i = 0; i < result.length; ++i) {
			result[i] = Math.pow(vector[i], power);
		}
		return result;
	}
	
	public static double[][] powComponentWise(double[][] matrix, double power) {
		double[][] result = new double[matrix.length][matrix[0].length];
		for (int row = 0; row < result.length; ++row) {
			for (int col = 0; col < result[0].length; ++col) {
				result[row][col] = Math.pow(matrix[row][col], power);
			}
		}
		return result;
	}
	
	public static double[] expComponentWise(double[] vector) {
		double[] result = new double[vector.length];
		for (int i = 0; i < result.length; ++i) {
			result[i] = Math.exp(vector[i]);
//			result[i] = Math.expm1(vector[i]);
		}
		return result;
	}
	
	public static double[] logComponentWise(double[] vector) {
		double[] result = new double[vector.length];
		for (int i = 0; i < vector.length; ++i) {
			result[i] = Math.log(vector[i]);
		}
		return result;
	}
	
	public static double sumCoordinates(double[] vector) {
		double sum = 0;
		for (double coordinate : vector) {
			sum += coordinate;
		}
		return sum;
	}
	
	public static double max(double[] vector) {
		double max = Double.NEGATIVE_INFINITY;
		for (double coordinate : vector) {
			if (max < coordinate) {
				max = coordinate;
			}
		}
		return max;
	}
	
	public static double[] subtract(double[] vector, double number) {
		double[] result = new double[vector.length];
		for (int i = 0; i < result.length; ++i) {
			result[i] = vector[i] - number;
		}
		return result;
	}
	
	public static int maxCoordinateIndex(double[] vector) {
		double max = Double.NEGATIVE_INFINITY;
		int maxIndex = 0;
		for (int index = 0; index < vector.length; ++index) {
			if (max < vector[index]) {
				max = vector[index];
				maxIndex = index;
			}
		}
		return maxIndex;
	}
	
	private static double scale(double number, double oldMin, double oldMax, double newMin, double newMax) {
		if (number < oldMin) {
			return newMin;
		}
		if (number > oldMax) {
			return newMax;
		}
		double t = (number - oldMin) / (oldMax - oldMin);
		return (1 - t) * newMin + t * newMax;
	}
}
