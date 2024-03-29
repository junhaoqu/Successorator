package edu.ucsd.cse110.successorator.lib.domain;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Comparator;

import edu.ucsd.cse110.successorator.lib.data.InMemoryDataSource;
import edu.ucsd.cse110.successorator.lib.util.Subject;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SimpleGoalRepository implements GoalRepository {
    private final InMemoryDataSource dataSource;



    public SimpleGoalRepository(InMemoryDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Subject<Goal> find(int id) {
        return dataSource.getGoalSubject(id);
    }

    @Override
    public Subject<List<Goal>> findAll() {
        return dataSource.getAllGoalsSubject();
    }

    @Override
    public void save(Goal goal) {
        dataSource.putGoal(goal);
    }

    @Override
    public void save(List<Goal> goals) {
        dataSource.putGoals(goals);
    }

    @Override
    public void remove(int id) {
        dataSource.removeGoal(id);
    }
    @Override
    public void append(Goal goal){
        //if goal name is not null, then add it to the end of the list
        if(goal.getName() != null){
            dataSource.putGoal(
                    goal.withSortOrder(dataSource.getMaxSortOrder()+1)
            );
        }
        else{
            throw new IllegalArgumentException("Goal name cannot be null");
        }
    }

    @Override
    public void prepend(Goal goal){
        //if goal name is not null, then add it to the front of the list
        if(goal.getName() != null){
            dataSource.shiftSortOrders(0, dataSource.getMaxSortOrder(), 1);
            dataSource.putGoal(
                    goal.withSortOrder(dataSource.getMinSortOrder() - 1)
            );
            //through error
        }
        else{
            throw new IllegalArgumentException("Goal name cannot be null");
        }

    }

    //generated by ChatGPT
    //Need some correction for category sort order to work
    @Override
    public void addGoalBetweenFinishedAndUnfinished(Goal goal) {
        // Get all the goals from the data source
        List<Goal> allGoals = dataSource.getGoals();

        // Find the index where the new goal should be inserted
        int insertIndex = 0;

        for (Goal existingGoal : allGoals) {
            // If the existing goal is finished, stop searching and insert behind it
            if (existingGoal.isFinished()) {
                break;
            }
            insertIndex++;
        }

        // Insert the new goal at the calculated index
        dataSource.shiftSortOrders(insertIndex, dataSource.getMaxSortOrder(), 1);
        dataSource.putGoal(goal.withSortOrder(dataSource.getMinSortOrder() + insertIndex));
    }


    @Override
    public void removeFinishedGoals() {
        dataSource.removeFinishedGoals();
    }

    public void addRecurringGoals() {
        // Get the current date
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        // Get all the goals from the data source
        List<Goal> allGoals = dataSource.getGoals();

        // Filter for recurring goals
        List<Goal> recurringGoals = allGoals.stream()
                .filter(goal -> goal.getRepeatInterval() != Goal.RepeatInterval.ONE_TIME)
                .collect(Collectors.toList());

        // For each recurring goal
        for (Goal goal : recurringGoals) {
            // Calculate the next occurrence date based on the repeat interval
            assert goal.getDate() != null;
            var oldId = goal.getId();
            calendar.setTime(goal.getDate());
            switch (goal.getRepeatInterval()) {
                case DAILY:
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                case WEEKLY:
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case MONTHLY:
                    calendar.add(Calendar.MONTH, 1);
                    break;
                case YEARLY:
                    calendar.add(Calendar.YEAR, 1);
                    break;
            }
            Date nextDate = calendar.getTime();

            // If the next occurrence is today or in the future, add a new goal to the repository
            if (goal.getDate().before(currentDate)) {
                Goal newGoal = new Goal(goal.getId(), goal.getName(), false, goal.sortOrder(), nextDate, goal.getRepeatInterval(), goal.getCategory());
                save(newGoal);
                remove(oldId);
            }
        }
    }

}