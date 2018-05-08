package enums;

public enum Banners {

    PAPERLESS_UTILITY_BANNER(
                    "______                            _                  _   _  _    _  _  _  _          \n" +
                    "| ___ \\                          | |                | | | || |  (_)| |(_)| |         \n" +
                    "| |_/ /  __ _  _ __    ___  _ __ | |  ___  ___  ___ | | | || |_  _ | | _ | |_  _   _ \n" +
                    "|  __/  / _` || '_ \\  / _ \\| '__|| | / _ \\/ __|/ __|| | | || __|| || || || __|| | | |\n" +
                    "| |    | (_| || |_) ||  __/| |   | ||  __/\\__ \\\\__ \\| |_| || |_ | || || || |_ | |_| |\n" +
                    "\\_|     \\__,_|| .__/  \\___||_|   |_| \\___||___/|___/ \\___/  \\__||_||_||_| \\__| \\__, |\n" +
                    "              | |                                                               __/ |\n" +
                    "              |_|                                                              |___/ "
    );

    private final String banner;

    Banners(String banner) {
        this.banner = banner;
    }

    @Override
    public String toString() {
        return banner;
    }
}
