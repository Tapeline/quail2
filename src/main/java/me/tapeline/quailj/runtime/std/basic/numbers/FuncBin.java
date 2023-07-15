package me.tapeline.quailj.runtime.std.basic.numbers;

import me.tapeline.quailj.lexing.TokenType;
import me.tapeline.quailj.parsing.nodes.literals.LiteralFunction;
import me.tapeline.quailj.runtime.Runtime;
import me.tapeline.quailj.typing.classes.QObject;
import me.tapeline.quailj.typing.modifiers.ModifierConstants;
import me.tapeline.quailj.typing.utils.FuncArgument;
import me.tapeline.quailj.typing.classes.utils.QBuiltinFunc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class FuncBin extends QBuiltinFunc {

    public FuncBin(Runtime runtime) {
        super(
                "bin",
                Collections.singletonList(
                        new FuncArgument(
                                "n",
                                QObject.Val(),
                                new int[] {ModifierConstants.NUM},
                                LiteralFunction.Argument.POSITIONAL
                        )
                ),
                runtime,
                runtime.getMemory(),
                false
        );
    }

    @Override
    public QObject action(Runtime runtime, HashMap<String, QObject> args, List<QObject> argList) {
        return QObject.Val(Integer.toBinaryString((int) args.get("n").numValue()));
    }

}