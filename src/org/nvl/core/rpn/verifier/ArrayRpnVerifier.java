/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package src.org.nvl.core.rpn.verifier;

import src.org.nvl.core.rpn.AbstractRpnVerifier;
import src.org.nvl.core.variable.Type;

import java.util.Stack;
import java.util.StringTokenizer;

import static src.org.nvl.MessageConstants.*;

/**
 * @author niki
 */
public class ArrayRpnVerifier extends AbstractRpnVerifier {

    @Override
    public String createRpn(String input) {
        StringBuilder result = new StringBuilder();   //builder for the final result (RPN)
        Stack<Character> operationStack = new Stack<>();  //sttack for the operation
        char[] charInput = input.toCharArray();  //char array for the input
        int i = 0;
        if (charInput[i] == '-' || charInput[i] == '+') {    //if the expresion begins with - or + (e.g. -52+3..)
            result.append(charInput[i]);
            i++;
        } //end of if
        while (i < charInput.length) {   //iterrate through the input
            switch (charInput[i]) {
                case '+':
                case '-':
                    while (!operationStack.empty() && operationStack.peek() != '(' && operationStack.peek() != ')') {   //if the previous operations in the stack have higher priorities
                        result.append(' ').append(operationStack.pop());                          // add them to result
                    }
                    operationStack.push(charInput[i]);
                    result.append(' ');
                    break;
                case '*':
                case '/':
                    while (!operationStack.empty() && (operationStack.peek() == '^' || operationStack.peek() == '*'
                            || operationStack.peek() == '/')) {   //if the previous operations in the stack have higher priorities
                        result.append(' ').append(operationStack.pop());                          // add them to result
                    }
                    operationStack.push(charInput[i]);
                    result.append(' ');
                    break;
                case '^':
                    while (!operationStack.empty() && operationStack.peek() == '^') {   //if the previous operations in the stack have higher or equal priorities
                        result.append(' ').append(operationStack.pop());                          // add them to result
                    }
                    operationStack.push(charInput[i]);
                    result.append(' ');
                    break;
                case '(':
                    operationStack.push(charInput[i]);
                    break;
                case ' ':
                    break;
                case ')':
                    while (!operationStack.empty() && operationStack.peek() != '(') {   // pop everything from stack to the result until we get to the '('
                        result.append(' ').append(operationStack.pop());
                    }
                    if (!operationStack.empty()) {    //remove the '('
                        operationStack.pop();
                    }
                    break;
                case '!':
                    throw new RuntimeException(INVALID_INPUT_MESSAGE);
                default:
                    result.append(charInput[i]);    // we have a digit
                    break;
            }  //end of switch
            i++;
        }  //end of while
        while (!operationStack.isEmpty()) {  //pop every operation from the stack to the result
            result.append(' ').append(operationStack.pop());
        }  //end of while
        return result.toString();  //return resulted RPN
    }

    @Override
    public String calculateRpn(String input) {
        StringTokenizer tokens = new StringTokenizer(input);  //tokenize the input by ' '
        Stack<String> stack = new Stack<>();  //stack for the numbers
        while (tokens.hasMoreTokens()) {   //while we have more tokens
            String current = tokens.nextToken();

            switch (current) {        //switch to see if current string is operation or string
                case "+":
                case "*":
                case "-":
                case "/":
                    execute(stack, current);
                    break;
                case "^":
                    concatenate(stack);
                    break;
                default:
                    stack.push(current);       //current is string
                    break;
            }   //end of switch
        }   //end of while
        if(stack.empty()) {
            throw new RuntimeException(EMPTY_STACK_MESSAGE);
        }
        return stack.pop();     //return result
    } //end of calculateRpn

    private void execute(Stack<String> stack, String operation) {  //execute + and *
        boolean leftIsNumber = false, rightIsNumber = false;        //if any of the operands is a number
        if(stack.empty()) {
            throw new RuntimeException(EMPTY_STACK_MESSAGE);
        }
        String right = stack.pop();     //right operand
        if(stack.empty()) {
            throw new RuntimeException(INVALID_INPUT_MESSAGE);
        }
        String left = stack.pop();      //left operand

        if (Type.isNumber(left))   //if the left operand is a number
        {
            leftIsNumber = true;
        }

        if (Type.isNumber(right))  //if the right operand is a number
        {
            rightIsNumber = true;
        }

        if (leftIsNumber && !rightIsNumber) {     //if the left operand is a number and the right one is an array
            stack.push(executeOperationNumberArray(right, left, operation));    //do the operation with array and number
            return;
        }

        if (!leftIsNumber && rightIsNumber) {      //if the left is an array and the right one is number
            stack.push(executeOperationNumberArray(left, right, operation));        //do the operation with array and number
            return;
        }

        stack.push(executeOperationArrays(left, right, operation));     //else both are arrays, so we do the operation with array
    }   //end of execute

    //executes the given operation(+ or *) over 2 integers
    private String executeOperation(String left, String right, String operation) {
        Integer r = Integer.parseInt(right);
        Integer l = Integer.parseInt(left);
        switch (operation) {
            case "+":
                return Integer.toString(l + r);
            case "-":
                if(l - r < 0) {
                    throw new RuntimeException(NEGATIVE_NUMBERS_MESSAGE);
                }
                return Integer.toString(l - r);
            case "/":
                if(right.equals(0)) {
                    throw new RuntimeException(ZERO_DIVISION_MESSAGE);
                }
                return Integer.toString(l / r);
            default:
                return Integer.toString(l * r);
        } //end of switch
    }   //end of executeOperation

    //execute for operation(+ or *) with array operand and number operand
    private String executeOperationNumberArray(String array, String number, String operation) {
        StringBuilder result = new StringBuilder();
        StringTokenizer tokens = new StringTokenizer(array.substring(1, array.length() - 1), ", ");       //with this tokenizer we can get every value in the array (, is token)
        result.append('{');     //the result will be an array so we start with '{'

        while (tokens.hasMoreTokens()) {      //while we have more numbers
            result.append(
                    executeOperation(tokens.nextToken(), number, operation));     //execute the operation over the number and the current element of the array(token) and save the result
            result.append(',');                                                         //append ',' to separate the elements in the array
        }       //end of while

        result.deleteCharAt(result.length() - 1);         //delete the last ',' added
        result.append('}');                     //add the closing '}'
        return result.toString();       //return result
    }   //end of executeOperationNumberArray

    //execute operation(+ or *) if we have two array operands
    private String executeOperationArrays(String leftArray, String rightArray, String operation) {
        StringBuilder result = new StringBuilder();
        StringTokenizer leftTokens = new StringTokenizer(leftArray.substring(1, leftArray.length() - 1), ", ");  //tokenize the first array by ',' (we get its elements)
        StringTokenizer rightTokens = new StringTokenizer(rightArray.substring(1, rightArray.length() - 1), ", ");    //tokenize the second array by ',' (we get its elements)
        result.append('{');     //the result will be an array so we start with '{'

        while (leftTokens.hasMoreTokens() && rightTokens.hasMoreTokens()) {       //while we have more tokens in both arrays
            result.append(executeOperation(leftTokens.nextToken(), rightTokens.nextToken(), operation));    //we do the operation over the corresponding el. of the arrays
            //and save the result
            result.append(',');                                                                             //than we separate the elements in the result
        }

        while (leftTokens.hasMoreTokens()) {         //if the left array has more elements than the right one
            result.append(leftTokens.nextToken());      //add the rest elements to the result
            result.append(',');                         //separate them
        }

        while (rightTokens.hasMoreTokens()) {        //if the right array --||--
            result.append(rightTokens.nextToken());
            result.append(',');
        }

        result.deleteCharAt(result.length() - 1);     //delete the last ','
        result.append('}');                     //close the array
        return result.toString();
    }   //end of executeOperationArrays

    //concatenates both arrays if we have ^ operation
    private void concatenate(Stack<String> stack) {
        String right = stack.pop(); //right array
        if(stack.empty()) {
            throw new RuntimeException(INVALID_INPUT_MESSAGE);
        }
        String left = stack.pop();  //left array

        if(!Type.isArray(right) || !Type.isArray(left)) {
            throw new RuntimeException(CONCATENATION_ERROR_MESSAGE);
        }

        StringBuilder result = new StringBuilder(left.substring(0, left.length() - 1));     //get the first element, without the closing } in the result
        result.append(',');                                                                 //separate its last element with a ,
        result.append(right.substring(1, right.length()));                                  //append the second array without the opening {
        stack.push(result.toString());                                                      //push result to stack
    }//end of concatenate
}
