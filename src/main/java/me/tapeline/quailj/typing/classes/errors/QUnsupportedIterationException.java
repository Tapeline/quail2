package me.tapeline.quailj.typing.classes.errors;

import me.tapeline.quailj.runtime.Runtime;
import me.tapeline.quailj.runtime.RuntimeStriker;
import me.tapeline.quailj.runtime.Table;
import me.tapeline.quailj.typing.classes.QObject;
import me.tapeline.quailj.utils.Dict;
import me.tapeline.quailj.utils.Pair;

public class QUnsupportedIterationException extends QException {

    public static final String OPERAND_FIELD = "operand";

    public static QUnsupportedIterationException prototype = new QUnsupportedIterationException(
            new Table(
                    Dict.make(
                            new Pair<>(OPERAND_FIELD, QObject.Val())
                    )
            ),
            "UnsupportedConversionException",
            QException.prototype,
            true
    );

    public QUnsupportedIterationException(Table table, String className, QObject parent, boolean isPrototype) {
        super(table, className, parent, isPrototype);
    }

    public QUnsupportedIterationException(Table table, String className,
                                          QObject parent, boolean isPrototype,
                                          String message, QObject left) {
        super(table, className, parent, isPrototype, message);
        set(OPERAND_FIELD, left, null);
    }

    public QUnsupportedIterationException(QObject left) {
        this(
                new Table(),
                prototype.className,
                prototype,
                false,
                "Unsupported iteration through " + left.getClassName(),
                left
        );
    }

    @Override
    public QObject derive() throws RuntimeStriker {
        if (!isPrototype)
            Runtime.error("Attempt to derive from non-prototype value");
        return new QUnsupportedIterationException(new Table(), className, this, false);
    }

    @Override
    public QObject extendAs(String className) throws RuntimeStriker {
        if (!isPrototype)
            Runtime.error("Attempt to inherit from non-prototype value");
        return new QUnsupportedIterationException(new Table(), className, this, true);
    }

    @Override
    public QObject copy() {
        QObject copy = new QUnsupportedIterationException(table, className, parent, isPrototype);
        copy.setInheritableFlag(isInheritable);
        return copy;
    }

}
