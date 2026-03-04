rootProject.name = "stratuscloud-backend"

include(
    ":apps:api",
    ":modules:common",
    ":modules:iam",
    ":modules:compute",
    ":modules:network",
    ":modules:storage",
    ":modules:governance",
    ":modules:audit"
)
