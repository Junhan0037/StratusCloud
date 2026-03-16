rootProject.name = "stratuscloud-backend"

include(
    ":apps:api",
    ":modules:common",
    ":modules:iam",
    ":modules:compute",
    ":modules:data",
    ":modules:network",
    ":modules:storage",
    ":modules:governance",
    ":modules:audit"
)
