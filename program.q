use "lang/ji" = ji

function __dummy() {}
__runtime = ji.JavaObject.pack(__dummy).boundRuntime

String = ji.getClass("java.lang.String")
Character = ji.getClass("java.lang.Character")
s = String("abc")
print(Character(s.charAt(2)).toString())

Math = ji.getClass("java.lang.Math")
print(Math.log10(1000))

# expected:
# This is my stringThis is my string2
return __buf