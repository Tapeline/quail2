package me.tapeline.quailj.typing.classes;

import me.tapeline.quailj.runtime.RuntimeStriker;

public interface CustomRangeGenerator {

    QObject createCustomRange(QObject endObject, QObject stepObject) throws RuntimeStriker;

}
