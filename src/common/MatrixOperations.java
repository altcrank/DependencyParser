package common;

import java.util.Random;

public class MatrixOperations {
	
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
			for (int col = 0; col < result[0].length; ++col)
			result[row][col] = MatrixOperations.scale(random.nextDouble(), 0, 1, lowerLimit, upperLimit);
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
	
	public static double[] sum(double[] vector1, double[] vector2) {
		if (vector1.length != vector2.length) {
			throw new RuntimeException("MatrixOperations::dotProduct: vectors have different sizes");
		}
		double[] result = new double[vector1.length];
		for (int i = 0; i < result.length; ++i) {
			result[i] = vector1[i] + vector2[i];
		}
		return result;
	}

	public static double[] pow(double[] vector, double power) {
		double[] result = new double[vector.length];
		for (int i = 0; i < result.length; ++i) {
			result[i] = Math.pow(vector[i], power);
		}
		return result;
	}
	
	public static double[] exp(double[] vector) {
		double[] result = new double[vector.length];
		for (int i = 0; i < result.length; ++i) {
			result[i] = Math.exp(vector[i]);
//			result[i] = Math.expm1(vector[i]);
		}
		return result;
	}
	
	public static int maxCoordinateIndex(double[] vector) {
		double max = 0;
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
		double t = (number - oldMin) / (oldMax - oldMin);
		return (1 - t) * newMin + t * newMax;
	}
}
