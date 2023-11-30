import os
import tempfile
import unittest
from unittest.mock import patch

from main import *

class TestMain(unittest.TestCase):
    def test_read_fitness_evolution_file(self):
        # Test reading a file with multiple lines
        with patch("builtins.open", create=True) as mock_open:
            mock_open.return_value.__enter__.return_value.readlines.return_value = [
                "line1\n",
                "line2\n",
            ]
            result = read_fitness_evolution_file("test_file.txt")
            self.assertEqual(result, ["line1\n", "line2\n"])

        # Test reading an empty file
        with patch("builtins.open", create=True) as mock_open:
            mock_open.return_value.__enter__.return_value.readlines.return_value = []
            result = read_fitness_evolution_file("empty_file.txt")
            self.assertEqual(result, [])

        # Test reading a file with a single line
        with patch("builtins.open", create=True) as mock_open:
            mock_open.return_value.__enter__.return_value.readlines.return_value = [
                "only line\n"
            ]
            result = read_fitness_evolution_file("single_line_file.txt")
            self.assertEqual(result, ["only line\n"])

    def test_read_fun_file(self):
        with tempfile.NamedTemporaryFile(suffix=".csv", delete=False) as temp_file:
            temp_file.write(b"col1,col2,col3\n1,2,3\n4,5,6\n")
            temp_file.flush()
            file_path = temp_file.name

            # Test case 1
            objectives = ["col2"]
            expected_output = pd.DataFrame({"col2": [2.0, 5.0]})
            assert read_fun_file(file_path, objectives).equals(expected_output)

            # Test case 2
            objectives = ["col3", "col1", "col2"]
            expected_output = pd.DataFrame(
                {"col3": [3.0, 6.0], "col1": [1.0, 4.0], "col2": [2.0, 5.0]}
            )
            assert read_fun_file(file_path, objectives).equals(expected_output)

    def test_plot_fitness_evolution(self):
        # Test case 1
        fitness_evolution_lines = ["1.0, 2.0, 3.0"]
        objectives = ["Objective 1"]
        output_file = "test_output.html"

        plot_fitness_evolution(fitness_evolution_lines, objectives, output_file)
        assert os.path.exists(output_file)
        os.remove(output_file)

        # Test case 2
        fitness_evolution_lines = ["1.0, 2.0, 3.0", "4.0, 5.0, 6.0"]
        objectives = ["Objective 1", "Objective 2"]
        output_file = "test_output.html"

        plot_fitness_evolution(fitness_evolution_lines, objectives, output_file)
        assert os.path.exists(output_file)
        os.remove(output_file)

        # Test case 3
        fitness_evolution_lines = [
            "1.0, 2.0, 3.0",
            "4.0, 5.0, 6.0",
            "7.0, 8.0, 9.0",
            "10.0, 11.0, 12.0",
        ]
        objectives = ["Objective 1", "Objective 2", "Objective 3", "Objective 4"]
        output_file = "test_output.html"

        plot_fitness_evolution(fitness_evolution_lines, objectives, output_file)
        assert os.path.exists(output_file)
        os.remove(output_file)

    def test_plot_parallel_coordinates(self):
        # Test case 1: Check if the function saves the output graph file correctly
        fun_df = pd.DataFrame({"A": [1, 2, 3], "B": [4, 5, 6], "C": [7, 8, 9]})
        objectives = ["A", "B", "C"]
        output_file = "test_output.html"

        plot_parallel_coordinates(fun_df, objectives, output_file)

        assert os.path.exists(output_file)
        os.remove(output_file)

        # Test case 2: Check if the function raises an error when the DataFrame is empty
        fun_df = pd.DataFrame()
        objectives = ["A", "B", "C"]
        output_file = "test_output.html"

        with self.assertRaises(ValueError):
            plot_parallel_coordinates(fun_df, objectives, output_file)

    def test_plot_2D_pareto_front(self):
        # Create a sample DataFrame
        data = {"Objective1": [1, 2, 3, 4, 5], "Objective2": [1, 4, 9, 16, 25]}
        df = pd.DataFrame(data)

        # Define the objectives and output file
        objectives = ["Objective1", "Objective2"]
        output_file = "pareto_front.html"

        # Call the function
        plot_2D_pareto_front(df, objectives, output_file)

        # Check if the output file exists
        assert os.path.exists(output_file)
        os.remove(output_file)

    def test_plot_3D_pareto_front(self):
        # Create a sample DataFrame
        data = {
            "Objective1": [1, 2, 3, 4, 5],
            "Objective2": [1, 4, 9, 16, 25],
            "Objective3": [1, 3, 7, 14, 21],
        }
        df = pd.DataFrame(data)

        # Define the objectives and output file
        objectives = ["Objective1", "Objective2", "Objective3"]
        output_file = "pareto_front.html"

        # Call the function
        plot_3D_pareto_front(df, objectives, output_file)

        # Check if the output file exists
        assert os.path.exists(output_file)
        os.remove(output_file)


if __name__ == "__main__":
    unittest.main()
