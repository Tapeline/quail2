package me.tapeline.quailj.runtime.std.reflect;

import me.tapeline.quailj.parsing.nodes.literals.LiteralFunction;
import me.tapeline.quailj.runtime.Runtime;
import me.tapeline.quailj.typing.classes.QBool;
import me.tapeline.quailj.typing.modifiers.ModifierConstants;
import me.tapeline.quailj.typing.classes.QObject;
import me.tapeline.quailj.typing.utils.FuncArgument;
import me.tapeline.quailj.typing.classes.utils.QBuiltinFunc;
import me.tapeline.quailj.runtime.RuntimeStriker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ReflectSetBoolValue extends QBuiltinFunc {

    public ReflectSetBoolValue(Runtime runtime) {
        super(
                "setBoolValue",
                Arrays.asList(
                        new FuncArgument(
                               "obj",
                                QObject.Val(),
                                new int[] {ModifierConstants.BOOL},
                                LiteralFunction.Argument.POSITIONAL
                        ),
                        new FuncArgument(
                               "value",
                                QObject.Val(),
                                new int[] {ModifierConstants.BOOL},
                                LiteralFunction.Argument.POSITIONAL
                        )
                ),
                runtime,
                runtime.getMemory(),
                false
        );
    }

    @Override
    public QObject action(Runtime runtime, HashMap<String, QObject> args, List<QObject> argList) throws RuntimeStriker {
        ((QBool) args.get("obj")).setValue(args.get("value").boolValue());
        return Val();
    }

}
