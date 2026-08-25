package org.example.service;

public class CalcServiceImpl implements CalcService{
    @Override
    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public int multiply(int a, int b) {
        return a * b;
    }

    @Override
    public double divide(double a, double b) {
        return a / b;
    }
    @Override
    public int boom() {
        throw new RuntimeException("我炸了");
    }

    @Override
    public int show(int a) {
        try{
            Thread.sleep(2000);
        }catch (InterruptedException e){

        }
        return a * 2;
    }

}
