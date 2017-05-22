/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package src.org.nvl.core.rpn;

/**
 * @author niki
 */
public abstract class AbstractRpnVerifier  {

    public abstract String createRpn(String input);

    public abstract String calculateRpn(String input);

    public boolean correct(StringBuilder builder) {
        String input = builder.toString();
        String operation = parseOperation(input);   //determine the operation (==, <=, >, !=)

        String[] split = input.split(operation);   //split by the operation
        String leftExpression = split[0].trim();            //left expression
        String rightExpression = split[1].trim();           //right expression

        String leftRpn = createRpn(leftExpression);           //RPN for the left expression
        String rightRpn = createRpn(rightExpression);        //RPN for the right expression

        String left = calculateRpn(leftRpn);       //left string result
        String right = calculateRpn(rightRpn);     //right string result
        return compare(left, right, operation);             //compare them with the operation
    }

    //execute operation
    protected <E extends Comparable<E>> boolean compare(E left, E right, String operation) {
        switch (operation) {
            case "==":
                return left.compareTo(right) == 0;
            case ">":
                return left.compareTo(right) > 0;
            case "<":
                return left.compareTo(right) < 0;
            case ">=":
                return left.compareTo(right) >= 0;
            case "<=":
                return left.compareTo(right) <= 0;
            case "!=":
                return left.compareTo(right) != 0;
            default:
                return false;
        }   //end of switch
    }   //end of compare

    //finds the operation of the statement
    protected String parseOperation(String input) {
        char[] charInput = input.toCharArray();
        int i = 0;
        while (charInput[i] != '=' && charInput[i] != '>' && charInput[i] != '<' && (charInput[i] != '!' || charInput[i + 1] != '=')) {  //iterrate while input[i] is not an operation symbol
            i++;
        }
        if (charInput[i + 1] != '=') {    //if operation is > or <
            return input.substring(i, i + 1);
        } else {    //operation is  >= or <= or  == or !=
            return input.substring(i, i + 2);
        }//end if
    } //end of parseOperation
}
