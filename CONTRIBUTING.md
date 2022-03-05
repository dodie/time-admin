# Contributing

Contributions are welcome! Please make sure to visit our
[development guide](https://github.com/dodie/time-admin/blob/master/development-guide.md)
for details about the application.


## Step 1. Fork and clone the repo

```
git clone git@github.com:your-username/time-admin.git
```

## Step 2. Compile and run the tests

```
cd time-admin
sbt test it:test
```

The test suite contain end-to-end tests that exercise the whole application through a real browser.
You can run them with ```sbt e2e:test```.


## Step 3. Start the application

Execute the following command:
```
cd time-admin
sbt
```

Then in the SBT shell, run:
```
container:start
```

Launch a web browser on [http://localhost:8080](http://localhost:8080) and log in to Timeadmin with the default user
displayed on the login page.


## Step 4. Make your changes
Make your changes then verify that the application works correctly and pass the test suite (Step 2).

Please use the following guidelines:

- Make sure to respect existing formatting conventions. (Follow the same coding style as the code that you are modifying.)
- Update documentation for the change you are making.
- Add tests for your modifications where possible.
- Write descriptive commit messages and add each logical change to a separate commit to make code review easier.


## Step 5. Push and [submit a Pull Request](https://github.com/dodie/scott/compare/)
Done! Now it's my turn to review your PR and get in touch with you. :)
