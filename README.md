﻿# statement-verifier

Верификатор за прости свойства

Примерни свойства, които се проверяват:
- равенства за интегрални типове (x == 3, c == 'A')
- линейни неравенства (x > 3, 2y + 7 < 3y)
- съждителни логически операции (конюнкция, дизюнкция, отрицание) (b && !c)

Поддържат се числа, низове, булеви променливи и числови масиви. Възможно е да се 
добавят променливи (пример - а = 5 или 2 * а + 3 = 15) и след това да се използват
за проверяване на верността на сравнения (пример - 11 + 2 * а - 15 > 21). Възможни
са и сравнения с недефинирани предварително променливи (пример 2 * а + 3 > 1 е винаги
вярно, докато 2 * а + 4 < 15 е недефинирано, т.е. може да е истина, може да е лъжа). 
За повече примери - statement-verifier/test/org/nvl/core.
